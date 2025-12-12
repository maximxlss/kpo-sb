use actix_multipart::form::{MultipartForm, tempfile::TempFile, text::Text};
use actix_web::{HttpResponse, Responder, get, post, web};
use db::{self, DbPool};
use serde::Serialize;
use tracing::error;
use uuid::Uuid;

use crate::clients::{AnalyzerClient, FileStorageClient, WordCloudClient};
use crate::config::WORD_CLOUD_MAX_BYTES;

#[derive(MultipartForm)]
pub struct CreateWorkForm {
    #[multipart(limit = "100MB")]
    file: TempFile,
    author_name: Text<String>,
    assignment_name: Text<String>,
    comment: Text<String>,
}

#[derive(Serialize)]
struct CreateWorkResponse {
    id: Uuid,
}

#[derive(Serialize)]
#[serde(rename_all = "snake_case")]
enum ReportStatus {
    Running,
    Pending,
    Failed,
    Completed,
}

#[derive(Serialize)]
struct ReportResponse {
    status: ReportStatus,
    #[serde(skip_serializing_if = "Option::is_none")]
    is_plagiarism: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    error: Option<String>,
    matches: Vec<Uuid>,
}

#[post("/works")]
pub async fn create_work(
    web::ThinData(db_pool): web::ThinData<DbPool>,
    file_storage: web::Data<FileStorageClient>,
    analyzer_client: web::Data<AnalyzerClient>,
    form: MultipartForm<CreateWorkForm>,
) -> actix_web::Result<impl Responder> {
    validate_new_work(&form)?;

    let hash = file_storage
        .upload(&form.file)
        .await
        .map_err(|err| bad_gateway("file_storage upload failed", err))?;

    let work_id = db::works::insert_work(
        &db_pool,
        &form.author_name,
        &form.assignment_name,
        &form.comment,
        &hash,
    )
    .await
    .map_err(|err| internal("db insert work failed", err))?;

    if let Err(err) = analyzer_client.trigger(work_id).await {
        error!(error = ?err, "work_analyzer trigger failed");
    }

    Ok(HttpResponse::Created().json(CreateWorkResponse { id: work_id }))
}

#[post("/works/{id}/reanalyze")]
pub async fn reanalyze_work(
    web::ThinData(db_pool): web::ThinData<DbPool>,
    analyzer_client: web::Data<AnalyzerClient>,
    id: web::Path<Uuid>,
) -> actix_web::Result<impl Responder> {
    let work_id = id.into_inner();

    if !db::works::exists(&db_pool, work_id)
        .await
        .map_err(|err| internal("db exists check failed", err))?
    {
        return Err(actix_web::error::ErrorNotFound("Work not found"));
    }

    db::analysis_results::clear(&db_pool, work_id)
        .await
        .map_err(|err| internal("db clear analysis results failed", err))?;

    analyzer_client
        .trigger(work_id)
        .await
        .map_err(|err| bad_gateway("work_analyzer trigger failed", err))?;

    Ok(HttpResponse::Accepted().finish())
}

#[get("/works/{id}/report")]
pub async fn get_report(
    web::ThinData(db_pool): web::ThinData<DbPool>,
    analyzer_client: web::Data<AnalyzerClient>,
    id: web::Path<Uuid>,
) -> actix_web::Result<impl Responder> {
    get_report_by_id(db_pool, analyzer_client, id.into_inner()).await
}

async fn get_report_by_id(
    db_pool: DbPool,
    analyzer_client: web::Data<AnalyzerClient>,
    work_id: Uuid,
) -> actix_web::Result<impl Responder> {
    match report_state(&db_pool, work_id).await? {
        ReportState::Running => Ok(HttpResponse::Accepted().json(ReportResponse {
            status: ReportStatus::Running,
            is_plagiarism: None,
            error: None,
            matches: Vec::new(),
        })),
        ReportState::Pending => {
            if let Err(err) = analyzer_client.trigger(work_id).await {
                error!(error = ?err, "work_analyzer trigger failed while pending");
            }
            Ok(HttpResponse::Accepted().json(ReportResponse {
                status: ReportStatus::Pending,
                is_plagiarism: None,
                error: None,
                matches: Vec::new(),
            }))
        }
        ReportState::Failed(message) => Ok(HttpResponse::Ok().json(ReportResponse {
            status: ReportStatus::Failed,
            is_plagiarism: None,
            error: Some(message),
            matches: Vec::new(),
        })),
        ReportState::Completed(matches) => Ok(HttpResponse::Ok().json(ReportResponse {
            status: ReportStatus::Completed,
            is_plagiarism: Some(!matches.is_empty()),
            error: None,
            matches,
        })),
    }
}

enum ReportState {
    Running,
    Pending,
    Failed(String),
    Completed(Vec<Uuid>),
}

async fn report_state(db_pool: &DbPool, work_id: Uuid) -> actix_web::Result<ReportState> {
    if !db::works::exists(db_pool, work_id)
        .await
        .map_err(|err| internal("db exists check failed", err))?
    {
        return Err(actix_web::error::ErrorNotFound("Work not found"));
    }

    let results = db::analysis_results::for_work(db_pool, work_id)
        .await
        .map_err(|err| internal("db load analysis results failed", err))?;

    if results.is_empty() {
        return Ok(ReportState::Pending);
    }

    if results.iter().any(|r| r.is_running) {
        return Ok(ReportState::Running);
    }

    if let Some(row) = results.iter().find(|r| r.error_message.is_some()) {
        return Ok(ReportState::Failed(
            row.error_message
                .clone()
                .unwrap_or_else(|| "Analysis failed".to_string()),
        ));
    }

    let matches: Vec<Uuid> = results
        .into_iter()
        .filter_map(|r| r.stolen_from_id)
        .collect();

    Ok(ReportState::Completed(matches))
}

#[get("/works/{id}/wordcloud")]
pub async fn get_word_cloud(
    web::ThinData(db_pool): web::ThinData<DbPool>,
    file_storage: web::Data<FileStorageClient>,
    word_cloud_client: web::Data<WordCloudClient>,
    id: web::Path<Uuid>,
) -> actix_web::Result<impl Responder> {
    let work_id = id.into_inner();

    let hash = db::works::fetch_file_hash(&db_pool, work_id)
        .await
        .map_err(|err| internal("db fetch file hash failed", err))?
        .ok_or_else(|| actix_web::error::ErrorNotFound("Work not found"))?;

    let file_bytes = file_storage
        .download_by_hash(&hash)
        .await
        .map_err(|err| bad_gateway("file_storage download failed", err))?;

    if file_bytes.is_empty() {
        return Err(actix_web::error::ErrorBadRequest("Work is empty"));
    }
    if file_bytes.len() > WORD_CLOUD_MAX_BYTES {
        return Err(actix_web::error::ErrorPayloadTooLarge(
            "Work file is too large for a word cloud (512KB limit)",
        ));
    }

    let text = String::from_utf8(file_bytes.to_vec()).map_err(|_| {
        actix_web::error::ErrorUnsupportedMediaType(
            "Work file must be UTF-8 text to render a word cloud",
        )
    })?;

    if text.trim().is_empty() {
        return Err(actix_web::error::ErrorBadRequest(
            "Work does not contain text for a word cloud",
        ));
    }

    let image = word_cloud_client
        .render(&text)
        .await
        .map_err(|err| bad_gateway("word cloud provider request failed", err))?;

    Ok(HttpResponse::Ok().content_type("image/png").body(image))
}

fn validate_new_work(form: &CreateWorkForm) -> actix_web::Result<()> {
    if form.author_name.trim().is_empty() || form.assignment_name.trim().is_empty() {
        return Err(actix_web::error::ErrorBadRequest(
            "author_name and assignment_name are required",
        ));
    }

    Ok(())
}

fn internal<E: std::fmt::Debug>(context: &'static str, err: E) -> actix_web::Error {
    error!(error = ?err, "{context}");
    actix_web::error::ErrorInternalServerError("Internal server error")
}

fn bad_gateway<E: std::fmt::Debug>(context: &'static str, err: E) -> actix_web::Error {
    error!(error = ?err, "{context}");
    actix_web::error::ErrorBadGateway("Upstream service error")
}
