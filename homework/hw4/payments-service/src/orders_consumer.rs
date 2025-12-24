use futures_util::StreamExt;
use lapin::{
    options::{
        BasicAckOptions, BasicConsumeOptions, BasicNackOptions, BasicQosOptions,
        ExchangeDeclareOptions, QueueBindOptions, QueueDeclareOptions,
    },
    types::FieldTable,
    Connection, ConnectionProperties, ExchangeKind,
};
use serde::Deserialize;
use sqlx::PgPool;
use std::time::Duration;
use tracing::{error, info, warn};
use uuid::Uuid;

const EXCHANGE_NAME: &str = "events";
const QUEUE_NAME: &str = "payments.orders.created";

#[derive(Debug, Deserialize)]
struct OrderCreatedEvent {
    event_id: Uuid,
    order_id: Uuid,
    user_id: Uuid,
    amount_cents: i64,
}

pub async fn run(db: PgPool, amqp_url: String) {
    info!("orders consumer task started");
    loop {
        if let Err(err) = consume_loop(&db, &amqp_url).await {
            warn!("orders consumer error: {err:?}");
            tokio::time::sleep(Duration::from_secs(5)).await;
        }
    }
}

async fn consume_loop(db: &PgPool, amqp_url: &str) -> anyhow::Result<()> {
    let conn = Connection::connect(amqp_url, ConnectionProperties::default()).await?;
    let channel = conn.create_channel().await?;
    channel
        .exchange_declare(
            EXCHANGE_NAME,
            ExchangeKind::Topic,
            ExchangeDeclareOptions {
                durable: true,
                ..Default::default()
            },
            FieldTable::default(),
        )
        .await?;
    channel
        .queue_declare(
            QUEUE_NAME,
            QueueDeclareOptions {
                durable: true,
                ..Default::default()
            },
            FieldTable::default(),
        )
        .await?;
    channel
        .queue_bind(
            QUEUE_NAME,
            EXCHANGE_NAME,
            "orders.created",
            QueueBindOptions::default(),
            FieldTable::default(),
        )
        .await?;
    channel.basic_qos(10, BasicQosOptions::default()).await?;

    let mut consumer = channel
        .basic_consume(
            QUEUE_NAME,
            "payments-orders-consumer",
            BasicConsumeOptions::default(),
            FieldTable::default(),
        )
        .await?;

    while let Some(delivery) = consumer.next().await {
        let delivery = match delivery {
            Ok(delivery) => delivery,
            Err(err) => {
                return Err(err.into());
            }
        };

        let result = handle_delivery(db, &delivery.data).await;
        match result {
            Ok(()) => {
                delivery.ack(BasicAckOptions::default()).await?;
            }
            Err(err) => {
                error!("failed to handle orders.created: {err:?}");
                delivery
                    .nack(BasicNackOptions {
                        requeue: true,
                        ..Default::default()
                    })
                    .await?;
            }
        }
    }

    Ok(())
}

async fn handle_delivery(db: &PgPool, payload: &[u8]) -> anyhow::Result<()> {
    let event: OrderCreatedEvent = serde_json::from_slice(payload)?;
    let mut tx = db.begin().await?;

    let inbox_insert =
        sqlx::query("INSERT INTO inbox (event_id) VALUES ($1) ON CONFLICT DO NOTHING")
            .bind(event.event_id)
            .execute(&mut *tx)
            .await?;

    if inbox_insert.rows_affected() == 0 {
        tx.commit().await?;
        return Ok(());
    }

    let ledger_insert = sqlx::query(
        "INSERT INTO ledger (order_id, user_id, amount_cents, status) \
         VALUES ($1, $2, $3, 'PENDING') \
         ON CONFLICT (order_id) DO NOTHING",
    )
    .bind(event.order_id)
    .bind(event.user_id)
    .bind(event.amount_cents)
    .execute(&mut *tx)
    .await?;

    if ledger_insert.rows_affected() == 0 {
        sqlx::query("UPDATE inbox SET processed_at = now() WHERE event_id = $1")
            .bind(event.event_id)
            .execute(&mut *tx)
            .await?;
        tx.commit().await?;
        return Ok(());
    }

    let debit = sqlx::query(
        "UPDATE accounts \
         SET balance_cents = balance_cents - $1, updated_at = now() \
         WHERE user_id = $2 AND balance_cents >= $1",
    )
    .bind(event.amount_cents)
    .bind(event.user_id)
    .execute(&mut *tx)
    .await?;

    let status = if debit.rows_affected() == 1 {
        "FINISHED"
    } else {
        "CANCELED"
    };

    sqlx::query("UPDATE ledger SET status = $1 WHERE order_id = $2")
        .bind(status)
        .bind(event.order_id)
        .execute(&mut *tx)
        .await?;

    let outbox_event_id = Uuid::new_v4();
    let outbox_payload = serde_json::json!({
        "event_id": outbox_event_id,
        "order_id": event.order_id,
        "user_id": event.user_id,
        "amount_cents": event.amount_cents,
        "status": status,
    });

    sqlx::query("INSERT INTO outbox (event_id, event_type, payload) VALUES ($1, $2, $3)")
        .bind(outbox_event_id)
        .bind("payments.result")
        .bind(outbox_payload)
        .execute(&mut *tx)
        .await?;

    sqlx::query("UPDATE inbox SET processed_at = now() WHERE event_id = $1")
        .bind(event.event_id)
        .execute(&mut *tx)
        .await?;

    tx.commit().await?;
    Ok(())
}
