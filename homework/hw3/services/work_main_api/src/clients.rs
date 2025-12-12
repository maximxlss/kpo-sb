use actix_multipart::form::tempfile::TempFile;
use actix_web::http::header::{AUTHORIZATION, CONTENT_LENGTH, CONTENT_TYPE};
use actix_web::web::Bytes;
use anyhow::anyhow;
use awc::{Client, error::PayloadError};
use futures_util::{StreamExt, TryStreamExt, stream};
use serde::{Deserialize, Serialize};
use tokio_util::io::ReaderStream;
use uuid::Uuid;

use crate::config::{
    ANALYZER_SERVICE_URL, FILE_STORAGE_SERVICE_URL, HASH_REGEX, WORD_CLOUD_ENDPOINT,
};

#[derive(Clone)]
pub struct FileStorageClient {
    base_url: String,
    password: String,
}

impl FileStorageClient {
    pub fn from_env() -> anyhow::Result<Self> {
        Ok(Self::new(
            FILE_STORAGE_SERVICE_URL,
            crate::config::file_storage_password()?,
        ))
    }

    pub fn new(base_url: impl Into<String>, password: String) -> Self {
        Self {
            base_url: base_url.into(),
            password,
        }
    }

    pub async fn upload(&self, file: &TempFile) -> anyhow::Result<String> {
        let path = file.file.path().to_path_buf();
        let filename = file
            .file_name
            .clone()
            .unwrap_or_else(|| "upload.bin".to_string());

        let client = Client::default();
        let file = tokio::fs::File::open(path).await?;
        let file_stream = ReaderStream::new(file).map_err(PayloadError::Io);

        let boundary = format!("upload-{}", Uuid::new_v4());
        let start = format!(
            "--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{filename}\"\r\nContent-Type: application/octet-stream\r\n\r\n"
        );
        let end = format!("\r\n--{boundary}--\r\n");

        let payload = stream::once(async { Ok(actix_web::web::Bytes::from(start)) })
            .chain(file_stream)
            .chain(stream::once(async { Ok(actix_web::web::Bytes::from(end)) }));

        let mut res = client
            .post(format!("{}/upload", self.base_url))
            .insert_header((
                CONTENT_TYPE,
                format!("multipart/form-data; boundary={boundary}"),
            ))
            .insert_header((AUTHORIZATION, format!("Bearer {}", self.password)))
            .send_stream(payload)
            .await
            .map_err(|err| anyhow!(err.to_string()))?;

        let status = res.status();
        let body_bytes = res.body().await.map_err(|err| anyhow!(err.to_string()))?;

        if !status.is_success() {
            return Err(anyhow!(
                "file_storage upload failed with status {} body {}",
                status,
                String::from_utf8_lossy(&body_bytes)
            ));
        }

        let body: UploadResponse = serde_json::from_slice(&body_bytes)?;
        if !HASH_REGEX.is_match(&body.hash) {
            return Err(anyhow!("file_storage returned an invalid hash"));
        }

        Ok(body.hash)
    }

    pub async fn download_by_hash(&self, hash: &str) -> anyhow::Result<Bytes> {
        if !HASH_REGEX.is_match(hash) {
            return Err(anyhow!("invalid hash"));
        }

        let client = Client::default();

        let mut res = client
            .get(format!("{}/download/{hash}", self.base_url))
            .insert_header((AUTHORIZATION, format!("Bearer {}", self.password)))
            .send()
            .await
            .map_err(|err| anyhow!(err.to_string()))?;

        let status = res.status();
        let body = res.body().await.map_err(|err| anyhow!(err.to_string()))?;
        if !status.is_success() {
            return Err(anyhow!(
                "file_storage download failed with status {} body {}",
                status,
                String::from_utf8_lossy(&body)
            ));
        }

        Ok(body)
    }
}

#[derive(Clone)]
pub struct AnalyzerClient {
    base_url: String,
}

impl AnalyzerClient {
    pub fn new(base_url: impl Into<String>) -> Self {
        Self {
            base_url: base_url.into(),
        }
    }

    pub async fn trigger(&self, work_id: Uuid) -> anyhow::Result<()> {
        let body = serde_json::to_vec(&AnalyzerRequest { work_id })?;
        let client = Client::default();
        let mut res = client
            .post(format!("{}/analyze", self.base_url))
            .insert_header((CONTENT_TYPE, "application/json"))
            .insert_header((CONTENT_LENGTH, body.len()))
            .send_body(body)
            .await
            .map_err(|err| anyhow!(err.to_string()))?;

        let status = res.status();
        if !status.is_success() {
            let body = res
                .body()
                .await
                .unwrap_or_else(|_| actix_web::web::Bytes::new());
            return Err(anyhow!(
                "analyzer responded with status {} body {}",
                status,
                String::from_utf8_lossy(&body)
            ));
        }

        Ok(())
    }
}

impl Default for AnalyzerClient {
    fn default() -> Self {
        Self::new(ANALYZER_SERVICE_URL)
    }
}

#[derive(Clone)]
pub struct WordCloudClient {
    endpoint: String,
}

impl WordCloudClient {
    pub fn new(endpoint: impl Into<String>) -> Self {
        Self {
            endpoint: endpoint.into(),
        }
    }

    pub async fn render(&self, text: &str) -> anyhow::Result<Bytes> {
        let payload = WordCloudRequest {
            format: "png",
            width: 800,
            height: 600,
            font_scale: 18,
            scale: "linear",
            remove_stopwords: true,
            text: text.to_owned(),
        };

        let client = Client::default();

        let mut res = client
            .post(&self.endpoint)
            .insert_header((CONTENT_TYPE, "application/json"))
            .send_body(serde_json::to_vec(&payload)?)
            .await
            .map_err(|err| anyhow!(err.to_string()))?;

        let status = res.status();
        let body = res.body().await.map_err(|err| anyhow!(err.to_string()))?;
        if !status.is_success() {
            return Err(anyhow!(
                "word cloud provider error {} body {}",
                status,
                String::from_utf8_lossy(&body)
            ));
        }

        Ok(body)
    }
}

impl Default for WordCloudClient {
    fn default() -> Self {
        Self::new(WORD_CLOUD_ENDPOINT)
    }
}

#[derive(Deserialize)]
struct UploadResponse {
    hash: String,
}

#[derive(Serialize)]
struct AnalyzerRequest {
    work_id: Uuid,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
struct WordCloudRequest {
    format: &'static str,
    width: u16,
    height: u16,
    font_scale: u8,
    scale: &'static str,
    remove_stopwords: bool,
    text: String,
}
