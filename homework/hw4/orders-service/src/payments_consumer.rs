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
const QUEUE_NAME: &str = "orders.payments.result";

#[derive(Debug, Deserialize)]
struct PaymentResultEvent {
    event_id: Uuid,
    order_id: Uuid,
    user_id: Uuid,
    amount_cents: i64,
    status: String,
}

pub async fn run(db: PgPool, amqp_url: String) {
    info!("payments result consumer task started");
    loop {
        if let Err(err) = consume_loop(&db, &amqp_url).await {
            warn!("payments result consumer error: {err:?}");
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
            "payments.result",
            QueueBindOptions::default(),
            FieldTable::default(),
        )
        .await?;
    channel.basic_qos(10, BasicQosOptions::default()).await?;

    let mut consumer = channel
        .basic_consume(
            QUEUE_NAME,
            "orders-payments-consumer",
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
                error!("failed to handle payments.result: {err:?}");
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
    let event: PaymentResultEvent = serde_json::from_slice(payload)?;
    let status = event.status.to_uppercase();

    let result = sqlx::query(
        "UPDATE orders SET status = $1 WHERE order_id = $2 AND user_id = $3 AND status = 'NEW'",
    )
    .bind(&status)
    .bind(event.order_id)
    .bind(event.user_id)
    .execute(db)
    .await?;

    if result.rows_affected() == 0 {
        info!(
            "order status already updated or missing (order_id={}, event_id={}, amount_cents={})",
            event.order_id, event.event_id, event.amount_cents
        );
    }

    Ok(())
}
