use lapin::{
    options::{BasicPublishOptions, ConfirmSelectOptions, ExchangeDeclareOptions},
    types::FieldTable,
    BasicProperties, Connection, ConnectionProperties, ExchangeKind,
};
use serde_json::Value;
use sqlx::{FromRow, PgPool};
use std::time::Duration;
use tracing::{info, warn};
use uuid::Uuid;

const EXCHANGE_NAME: &str = "events";

#[derive(Debug, FromRow)]
struct OutboxRow {
    id: i64,
    event_id: Uuid,
    event_type: String,
    payload: Value,
}

pub async fn run(db: PgPool, amqp_url: String) {
    info!("orders outbox publisher task started");
    loop {
        if let Err(err) = run_once(&db, &amqp_url).await {
            warn!("orders outbox publisher error: {err:?}");
            tokio::time::sleep(Duration::from_secs(5)).await;
        }
    }
}

async fn run_once(db: &PgPool, amqp_url: &str) -> anyhow::Result<()> {
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
        .confirm_select(ConfirmSelectOptions::default())
        .await?;

    loop {
        publish_batch(db, &channel).await?;
        tokio::time::sleep(Duration::from_secs(2)).await;
    }
}

async fn publish_batch(db: &PgPool, channel: &lapin::Channel) -> anyhow::Result<()> {
    let mut tx = db.begin().await?;
    let rows = sqlx::query_as::<_, OutboxRow>(
        "SELECT id, event_id, event_type, payload \
         FROM outbox \
         WHERE published_at IS NULL \
         ORDER BY id \
         FOR UPDATE SKIP LOCKED \
         LIMIT 50",
    )
    .fetch_all(&mut *tx)
    .await?;

    if rows.is_empty() {
        tx.commit().await?;
        return Ok(());
    }

    for row in rows {
        let payload = serde_json::to_vec(&row.payload)?;
        let properties = BasicProperties::default()
            .with_message_id(row.event_id.to_string().into())
            .with_delivery_mode(2);
        let confirm = channel
            .basic_publish(
                EXCHANGE_NAME,
                &row.event_type,
                BasicPublishOptions::default(),
                &payload,
                properties,
            )
            .await?
            .await?;
        if !confirm.is_ack() {
            return Err(anyhow::anyhow!(
                "publisher confirm not acked for event {}",
                row.event_id
            ));
        }
        sqlx::query(
            "UPDATE outbox SET published_at = now(), attempts = attempts + 1 WHERE id = $1",
        )
        .bind(row.id)
        .execute(&mut *tx)
        .await?;
    }

    tx.commit().await?;
    Ok(())
}
