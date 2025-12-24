mod api;
mod db;
mod outbox_publisher;
mod payments_consumer;

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
        .unwrap_or_else(|_| "postgres://orders:orders@localhost:5433/orders".to_string());
    let amqp_url = env::var("AMQP_URL").unwrap_or_else(|_| "amqp://localhost:5672".to_string());
    let http_addr = env::var("HTTP_ADDR").unwrap_or_else(|_| "0.0.0.0:8081".to_string());

    let pool = db::create_pool(&database_url).await?;
    sqlx::migrate!("./migrations").run(&pool).await?;
    let state = AppState { db: pool.clone() };

    tokio::spawn(outbox_publisher::run(pool.clone(), amqp_url.clone()));
    tokio::spawn(payments_consumer::run(pool.clone(), amqp_url));

    info!("orders-service listening on {}", http_addr);

    HttpServer::new(move || {
        App::new()
            .app_data(web::Data::new(state.clone()))
            .service(api::health)
            .service(api::create_order)
            .service(api::get_order)
            .service(api::list_orders)
    })
    .bind(http_addr)?
    .run()
    .await?;

    Ok(())
}
