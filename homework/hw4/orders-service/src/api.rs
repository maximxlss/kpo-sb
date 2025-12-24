use actix_web::{error::ErrorInternalServerError, get, post, web, HttpResponse, Result};
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use uuid::Uuid;

use crate::AppState;

#[derive(Debug, Deserialize)]
pub struct CreateOrderRequest {
    pub user_id: Uuid,
    pub amount_cents: i64,
    #[serde(default)]
    pub description: String,
}

#[derive(Debug, Serialize)]
pub struct CreateOrderResponse {
    pub order_id: Uuid,
    pub status: String,
}

#[derive(Debug, Deserialize)]
pub struct ListOrdersQuery {
    pub user_id: Uuid,
}

#[derive(Debug, Deserialize)]
pub struct OrderQuery {
    pub user_id: Uuid,
}

#[derive(Debug, Serialize, FromRow)]
pub struct OrderView {
    pub order_id: Uuid,
    pub user_id: Uuid,
    pub amount_cents: i64,
    pub description: String,
    pub status: String,
    pub created_at: DateTime<Utc>,
}

#[get("/health")]
pub async fn health() -> HttpResponse {
    HttpResponse::Ok().json(serde_json::json!({"status": "ok"}))
}

#[post("/orders")]
pub async fn create_order(
    state: web::Data<AppState>,
    payload: web::Json<CreateOrderRequest>,
) -> Result<HttpResponse> {
    let order_id = Uuid::new_v4();
    let event_id = Uuid::new_v4();

    let mut tx = state.db.begin().await.map_err(ErrorInternalServerError)?;

    sqlx::query(
        "INSERT INTO orders (order_id, user_id, amount_cents, description, status) \
         VALUES ($1, $2, $3, $4, 'NEW')",
    )
    .bind(order_id)
    .bind(payload.user_id)
    .bind(payload.amount_cents)
    .bind(&payload.description)
    .execute(&mut *tx)
    .await
    .map_err(ErrorInternalServerError)?;

    let outbox_payload = serde_json::json!({
        "event_id": event_id,
        "order_id": order_id,
        "user_id": payload.user_id,
        "amount_cents": payload.amount_cents
    });

    sqlx::query("INSERT INTO outbox (event_id, event_type, payload) VALUES ($1, $2, $3)")
        .bind(event_id)
        .bind("orders.created")
        .bind(outbox_payload)
        .execute(&mut *tx)
        .await
        .map_err(ErrorInternalServerError)?;

    tx.commit().await.map_err(ErrorInternalServerError)?;

    Ok(HttpResponse::Ok().json(CreateOrderResponse {
        order_id,
        status: "NEW".to_string(),
    }))
}

#[get("/orders/{order_id}")]
pub async fn get_order(
    state: web::Data<AppState>,
    order_id: web::Path<Uuid>,
    query: web::Query<OrderQuery>,
) -> Result<HttpResponse> {
    let order = sqlx::query_as::<_, OrderView>(
        "SELECT order_id, user_id, amount_cents, description, status, created_at \
         FROM orders WHERE order_id = $1 AND user_id = $2",
    )
    .bind(*order_id)
    .bind(query.user_id)
    .fetch_optional(&state.db)
    .await
    .map_err(ErrorInternalServerError)?;

    match order {
        Some(order) => Ok(HttpResponse::Ok().json(order)),
        None => Ok(HttpResponse::NotFound().finish()),
    }
}

#[get("/orders")]
pub async fn list_orders(
    state: web::Data<AppState>,
    query: web::Query<ListOrdersQuery>,
) -> Result<HttpResponse> {
    let orders = sqlx::query_as::<_, OrderView>(
        "SELECT order_id, user_id, amount_cents, description, status, created_at \
         FROM orders WHERE user_id = $1 ORDER BY created_at DESC",
    )
    .bind(query.user_id)
    .fetch_all(&state.db)
    .await
    .map_err(ErrorInternalServerError)?;

    Ok(HttpResponse::Ok().json(orders))
}
