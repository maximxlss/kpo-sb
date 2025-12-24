use actix_web::{error::ErrorInternalServerError, get, post, web, HttpResponse, Result};
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use uuid::Uuid;

use crate::AppState;

#[derive(Debug, Deserialize)]
pub struct CreateAccountRequest {
    pub user_id: Uuid,
}

#[derive(Debug, Deserialize)]
pub struct TopUpRequest {
    pub amount_cents: i64,
}

#[derive(Debug, Serialize, FromRow)]
pub struct AccountView {
    pub user_id: Uuid,
    pub balance_cents: i64,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}

#[get("/health")]
pub async fn health() -> HttpResponse {
    HttpResponse::Ok().json(serde_json::json!({"status": "ok"}))
}

#[post("/accounts")]
pub async fn create_account(
    state: web::Data<AppState>,
    payload: web::Json<CreateAccountRequest>,
) -> Result<HttpResponse> {
    let account = sqlx::query_as::<_, AccountView>(
        "INSERT INTO accounts (user_id, balance_cents) \
         VALUES ($1, 0) \
         ON CONFLICT (user_id) DO NOTHING \
         RETURNING user_id, balance_cents, created_at, updated_at",
    )
    .bind(payload.user_id)
    .fetch_optional(&state.db)
    .await
    .map_err(ErrorInternalServerError)?;

    match account {
        Some(account) => Ok(HttpResponse::Ok().json(account)),
        None => Ok(HttpResponse::Conflict().json(serde_json::json!({
            "error": "account already exists"
        }))),
    }
}

#[post("/accounts/{user_id}/topup")]
pub async fn topup(
    state: web::Data<AppState>,
    user_id: web::Path<Uuid>,
    payload: web::Json<TopUpRequest>,
) -> Result<HttpResponse> {
    let account = sqlx::query_as::<_, AccountView>(
        "UPDATE accounts \
         SET balance_cents = balance_cents + $1, updated_at = now() \
         WHERE user_id = $2 \
         RETURNING user_id, balance_cents, created_at, updated_at",
    )
    .bind(payload.amount_cents)
    .bind(*user_id)
    .fetch_optional(&state.db)
    .await
    .map_err(ErrorInternalServerError)?;

    match account {
        Some(account) => Ok(HttpResponse::Ok().json(account)),
        None => Ok(HttpResponse::NotFound().finish()),
    }
}

#[get("/accounts/{user_id}")]
pub async fn get_account(
    state: web::Data<AppState>,
    user_id: web::Path<Uuid>,
) -> Result<HttpResponse> {
    let account = sqlx::query_as::<_, AccountView>(
        "SELECT user_id, balance_cents, created_at, updated_at FROM accounts WHERE user_id = $1",
    )
    .bind(*user_id)
    .fetch_optional(&state.db)
    .await
    .map_err(ErrorInternalServerError)?;

    match account {
        Some(account) => Ok(HttpResponse::Ok().json(account)),
        None => Ok(HttpResponse::NotFound().finish()),
    }
}
