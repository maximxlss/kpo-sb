use std::sync::LazyLock;
use std::{io::ErrorKind, path::PathBuf};

use actix_files::NamedFile;
use actix_multipart::form::{MultipartForm, tempfile::TempFile};
use actix_web::{
    HttpResponse, Responder, get,
    http::header::{ContentDisposition, DispositionParam, DispositionType},
    post, web,
};
use db::{self, DbPool};
use regex::Regex;
use serde::Serialize;
use sha2::{Digest, Sha256};
use tracing::error;
use uuid::Uuid;

static HASH_REGEX: LazyLock<Regex> = LazyLock::new(|| Regex::new(r"^[a-fA-F0-9]{64}$").unwrap());
const STORAGE_DIR: &str = "./storage";

#[derive(MultipartForm)]
struct UploadForm {
    #[multipart(limit = "100MB")]
    file: TempFile,
}

#[derive(Serialize)]
struct UploadResponse {
    hash: String,
}

#[post("/upload", wrap = "password_auth::auth_middleware!()")]
async fn upload(mut form: MultipartForm<UploadForm>) -> actix_web::Result<impl Responder> {
    let file = form.0.file.file.as_file_mut();

    let mut hasher = Sha256::new();
    std::io::copy(file, &mut hasher).map_err(|err| internal("hash file read failed", err))?;
    let hash = format!("{:x}", hasher.finalize());

    if !HASH_REGEX.is_match_at(&hash, 0) {
        return Err(actix_web::error::ErrorInternalServerError(
            "Computed hash did not match expected format",
        ));
    }

    form.0
        .file
        .file
        .persist(storage_path(&hash))
        .map_err(|err| internal("persist uploaded file failed", err))?;

    Ok(HttpResponse::Ok().json(UploadResponse { hash }))
}

fn send_file_by_hash(hash: &str) -> actix_web::Result<NamedFile> {
    if !HASH_REGEX.is_match(hash) {
        return Err(actix_web::error::ErrorBadRequest("Invalid hash"));
    }

    let path = storage_path(hash);
    let file = NamedFile::open(path).map_err(|err| match err.kind() {
        ErrorKind::NotFound => actix_web::error::ErrorNotFound("File not found"),
        _ => internal("open stored file failed", err),
    })?;

    let content_disposition = ContentDisposition {
        disposition: DispositionType::Attachment,
        parameters: vec![DispositionParam::Filename(hash.into())],
    };

    Ok(file.set_content_disposition(content_disposition))
}

fn storage_path(hash: &str) -> PathBuf {
    PathBuf::from(STORAGE_DIR).join(hash.to_lowercase())
}

#[get("/download/{hash}", wrap = "password_auth::auth_middleware!()")]
async fn download(name: web::Path<String>) -> actix_web::Result<impl Responder> {
    send_file_by_hash(&name.into_inner())
}

#[get("/works/{id}/download")]
async fn download_work(
    web::ThinData(db_pool): web::ThinData<DbPool>,
    id: web::Path<Uuid>,
) -> actix_web::Result<impl Responder> {
    let work_id = id.into_inner();

    let hash = db::works::fetch_file_hash(&db_pool, work_id)
        .await
        .map_err(|err| internal("db fetch file hash failed", err))?
        .ok_or_else(|| actix_web::error::ErrorNotFound("Work not found"))?;

    send_file_by_hash(&hash)
}

fn internal<E: std::fmt::Debug>(context: &'static str, err: E) -> actix_web::Error {
    error!(error = ?err, "{context}");
    actix_web::error::ErrorInternalServerError("Internal server error")
}

#[actix_web::main]
async fn main() -> anyhow::Result<()> {
    LazyLock::force(&HASH_REGEX);
    logging::init();

    password_auth::check_for_env()?;
    db::check_for_env()?;

    let db_pool = web::ThinData(db::pg_pool_from_env().await?);

    let tmp_directory = PathBuf::from(STORAGE_DIR).join("tmp");
    std::fs::create_dir_all(&tmp_directory)?;
    let tempfile_config = web::Data::new(
        actix_multipart::form::tempfile::TempFileConfig::default().directory(tmp_directory),
    );

    tracing::info!("File storage listening...");
    actix_web::HttpServer::new(move || {
        actix_web::App::new()
            .app_data(db_pool.clone())
            .app_data(tempfile_config.clone())
            .wrap(logging::logger())
            .configure(healthcheck::configure)
            .service(upload)
            .service(download)
            .service(download_work)
    })
    .bind(("0.0.0.0", 8080))?
    .run()
    .await?;

    Ok(())
}
