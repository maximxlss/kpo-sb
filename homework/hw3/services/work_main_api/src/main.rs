mod clients;
mod config;
mod routes;

use actix_web::{App, HttpServer, web};
use tracing::info;

use crate::clients::{AnalyzerClient, FileStorageClient, WordCloudClient};

#[actix_web::main]
async fn main() -> anyhow::Result<()> {
    config::ensure_env()?;
    logging::init();

    db::check_for_env()?;
    let db_pool = web::ThinData(db::pg_pool_from_env().await?);
    let file_storage = web::Data::new(FileStorageClient::from_env()?);
    let analyzer_client = web::Data::new(AnalyzerClient::default());
    let word_cloud_client = web::Data::new(WordCloudClient::default());

    std::fs::create_dir_all("/tmp/uploads")?;
    let tempfile_config = web::Data::new(
        actix_multipart::form::tempfile::TempFileConfig::default().directory("/tmp/uploads"),
    );

    info!("Main work API listening...");
    HttpServer::new(move || {
        App::new()
            .app_data(db_pool.clone())
            .app_data(file_storage.clone())
            .app_data(analyzer_client.clone())
            .app_data(word_cloud_client.clone())
            .app_data(tempfile_config.clone())
            .wrap(logging::logger())
            .configure(healthcheck::configure)
            .configure(routes::configure)
    })
    .bind(("0.0.0.0", 8080))?
    .run()
    .await?;

    Ok(())
}
