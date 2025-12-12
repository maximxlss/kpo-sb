use regex::Regex;
use std::sync::LazyLock;

pub const WORD_CLOUD_MAX_BYTES: usize = 512 * 1024;
pub const FILE_STORAGE_SERVICE_URL: &str = "http://file_storage:8080";
pub const ANALYZER_SERVICE_URL: &str = "http://work_analyzer:8080";
pub const WORD_CLOUD_ENDPOINT: &str = "https://quickchart.io/wordcloud";

pub static HASH_REGEX: LazyLock<Regex> =
    LazyLock::new(|| Regex::new(r"^[a-fA-F0-9]{64}$").unwrap());

pub fn ensure_env() -> anyhow::Result<()> {
    LazyLock::force(&HASH_REGEX);
    file_storage_password().map(|_| ())
}

pub fn file_storage_password() -> anyhow::Result<String> {
    std::env::var("FILE_STORAGE_PASSWORD").map_err(|err| anyhow::anyhow!(err.to_string()))
}
