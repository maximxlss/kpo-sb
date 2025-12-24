mod api;
mod db;
mod orders_consumer;
mod outbox_publisher;

use actix_web::{web, App, HttpServer};
use sqlx::PgPool;
use std::env;
use tracing::info;

#[derive(Clone)]
pub struct AppState {
    pub db: PgPool,
}

#[actix_web::main]
async fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::from_default_env())
        .init();

    let database_url = env::var("DATABASE_URL")
        .unwrap_or_else(|_| "postgres://payments:payments@localhost:5434/payments".to_string());
    let amqp_url = env::var("AMQP_URL").unwrap_or_else(|_| "amqp://localhost:5672".to_string());
    let http_addr = env::var("HTTP_ADDR").unwrap_or_else(|_| "0.0.0.0:8082".to_string());

    let pool = db::create_pool(&database_url).await?;
    sqlx::migrate!("./migrations").run(&pool).await?;
    let state = AppState { db: pool.clone() };

    tokio::spawn(outbox_publisher::run(pool.clone(), amqp_url.clone()));
    tokio::spawn(orders_consumer::run(pool.clone(), amqp_url));

    info!("payments-service listening on {}", http_addr);

    HttpServer::new(move || {
        App::new()
            .app_data(web::Data::new(state.clone()))
            .service(api::health)
            .service(api::create_account)
            .service(api::topup)
            .service(api::get_account)
    })
    .bind(http_addr)?
    .run()
    .await?;

    Ok(())
}
