use actix_web::{HttpResponse, Responder, post, web};
use db::DbPool;
use serde::Deserialize;
use tracing::{error, info};
use uuid::Uuid;

#[derive(Deserialize)]
struct AnalyzeRequest {
    work_id: Uuid,
}

#[post("/analyze")]
async fn analyze(
    db_pool: web::Data<DbPool>,
    payload: web::Json<AnalyzeRequest>,
) -> actix_web::Result<impl Responder> {
    let pool = db_pool.get_ref().clone();
    let work_id = payload.work_id;

    // Ensure only one analysis per work runs at a time.
    if db::analysis_results::start_running(&pool, work_id)
        .await
        .map_err(actix_web::error::ErrorInternalServerError)?
    {
        actix_web::rt::spawn(async move {
            if let Err(err) = run_analysis(&pool, work_id).await {
                error!("analysis for work {} failed: {err:#}", work_id);
                let message = sanitize_error(&err.to_string());
                let _ = db::analysis_results::record_error(&pool, work_id, &message).await;
            }
        });
    }

    Ok(HttpResponse::Accepted().finish())
}

async fn run_analysis(pool: &DbPool, work_id: Uuid) -> anyhow::Result<()> {
    let Some(work) = db::works::fetch_metadata(pool, work_id).await? else {
        error!("work {} not found for analysis", work_id);
        return Ok(());
    };

    let matches = db::works::find_same_hash_other_authors(
        pool,
        &work.file_sha256,
        &work.author_name,
        work_id,
    )
    .await?;

    let rows: Vec<Option<Uuid>> = if matches.is_empty() {
        vec![None]
    } else {
        matches.into_iter().map(Some).collect()
    };

    db::analysis_results::replace_results(pool, work_id, &rows).await?;

    Ok(())
}

fn sanitize_error(raw: &str) -> String {
    let single_line = raw.replace('\n', " ");
    let trimmed = single_line.trim();
    const MAX_LEN: usize = 240;
    if trimmed.len() > MAX_LEN {
        trimmed[..MAX_LEN].to_string()
    } else {
        trimmed.to_string()
    }
}

#[actix_web::main]
async fn main() -> anyhow::Result<()> {
    logging::init();

    db::check_for_env()?;
    let db_pool = db::pg_pool_from_env().await?;

    info!("Work analyzer listening...");
    actix_web::HttpServer::new(move || {
        actix_web::App::new()
            .app_data(web::Data::new(db_pool.clone()))
            .wrap(logging::logger())
            .configure(healthcheck::configure)
            .service(analyze)
    })
    .bind(("0.0.0.0", 8080))?
    .run()
    .await?;

    Ok(())
}
