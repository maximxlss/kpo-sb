use sqlx::{PgPool, postgres::PgPoolOptions};
use std::sync::LazyLock;

use anyhow::Context as _;

pub type DbPool = PgPool;

const DEFAULT_MAX_CONNECTIONS: u32 = 5;
const DEFAULT_MAX_LIFETIME_SECONDS: u64 = 300;
pub const DATABASE_URL_ENV_VAR: &str = "DATABASE_URL";
static DATABASE_URL: LazyLock<anyhow::Result<String>> = LazyLock::new(|| {
    std::env::var(DATABASE_URL_ENV_VAR).context(format!("env {DATABASE_URL_ENV_VAR}"))
});

pub async fn pg_pool(database_url: &str) -> anyhow::Result<DbPool> {
    PgPoolOptions::new()
        .max_connections(DEFAULT_MAX_CONNECTIONS)
        .max_lifetime(std::time::Duration::from_secs(DEFAULT_MAX_LIFETIME_SECONDS))
        .connect(database_url)
        .await
        .map_err(Into::into)
}

pub async fn pg_pool_from_env() -> anyhow::Result<DbPool> {
    let database_url = match DATABASE_URL.as_ref() {
        Ok(url) => url,
        Err(err) => return Err(anyhow::anyhow!(err.to_string())),
    };
    pg_pool(database_url).await
}

pub fn check_for_env() -> anyhow::Result<()> {
    if let Err(err) = DATABASE_URL.as_ref() {
        return Err(anyhow::anyhow!("{err:#}"));
    }
    Ok(())
}

pub mod works {
    use super::DbPool;
    use uuid::Uuid;

    pub struct WorkMetadata {
        pub id: Uuid,
        pub author_name: String,
        pub file_sha256: String,
    }

    pub async fn fetch_file_hash(
        pool: &DbPool,
        work_id: Uuid,
    ) -> Result<Option<String>, sqlx::Error> {
        sqlx::query_scalar("SELECT file_sha256 FROM works WHERE id = $1")
            .bind(work_id)
            .fetch_optional(pool)
            .await
    }

    pub async fn fetch_metadata(
        pool: &DbPool,
        work_id: Uuid,
    ) -> Result<Option<WorkMetadata>, sqlx::Error> {
        sqlx::query_as::<_, (Uuid, String, String)>(
            "SELECT id, author_name, file_sha256 FROM works WHERE id = $1",
        )
        .bind(work_id)
        .fetch_optional(pool)
        .await
        .map(|row| {
            row.map(|(id, author_name, file_sha256)| WorkMetadata {
                id,
                author_name,
                file_sha256,
            })
        })
    }

    pub async fn exists(pool: &DbPool, work_id: Uuid) -> Result<bool, sqlx::Error> {
        sqlx::query_scalar("SELECT 1 FROM works WHERE id = $1")
            .bind(work_id)
            .fetch_optional(pool)
            .await
            .map(|opt: Option<i32>| opt.is_some())
    }

    pub async fn find_same_hash_other_authors(
        pool: &DbPool,
        file_sha256: &str,
        author_name: &str,
        work_id: Uuid,
    ) -> Result<Vec<Uuid>, sqlx::Error> {
        sqlx::query_scalar(
            "SELECT id FROM works WHERE file_sha256 = $1 AND author_name <> $2 AND id <> $3",
        )
        .bind(file_sha256)
        .bind(author_name)
        .bind(work_id)
        .fetch_all(pool)
        .await
    }

    pub async fn insert_work(
        pool: &DbPool,
        author_name: &str,
        assignment_name: &str,
        comment: &str,
        file_sha256: &str,
    ) -> Result<Uuid, sqlx::Error> {
        sqlx::query_scalar(
            "INSERT INTO works (author_name, assignment_name, comment, file_sha256) VALUES ($1, $2, $3, $4) RETURNING id",
        )
        .bind(author_name)
        .bind(assignment_name)
        .bind(comment)
        .bind(file_sha256)
        .fetch_one(pool)
        .await
    }
}

pub mod analysis_results {
    use super::DbPool;
    use uuid::Uuid;

    #[derive(Debug, Clone)]
    pub struct AnalysisRow {
        pub stolen_from_id: Option<Uuid>,
        pub error_message: Option<String>,
        pub is_running: bool,
    }

    pub async fn start_running(pool: &DbPool, work_id: Uuid) -> Result<bool, sqlx::Error> {
        let inserted = sqlx::query_scalar::<_, bool>(
            "INSERT INTO analysis_results (work_id, is_running) VALUES ($1, TRUE) ON CONFLICT (work_id) WHERE is_running DO NOTHING RETURNING TRUE",
        )
        .bind(work_id)
        .fetch_optional(pool)
        .await?;

        Ok(inserted.is_some())
    }

    pub async fn replace_results(
        pool: &DbPool,
        work_id: Uuid,
        stolen_from_ids: &[Option<Uuid>],
    ) -> Result<(), sqlx::Error> {
        let mut tx = pool.begin().await?;
        sqlx::query("DELETE FROM analysis_results WHERE work_id = $1")
            .bind(work_id)
            .execute(&mut *tx)
            .await?;

        for stolen_from_id in stolen_from_ids {
            sqlx::query("INSERT INTO analysis_results (work_id, stolen_from_id) VALUES ($1, $2)")
                .bind(work_id)
                .bind(stolen_from_id)
                .execute(&mut *tx)
                .await?;
        }

        tx.commit().await?;
        Ok(())
    }

    pub async fn record_error(
        pool: &DbPool,
        work_id: Uuid,
        error_message: &str,
    ) -> Result<(), sqlx::Error> {
        let mut tx = pool.begin().await?;
        sqlx::query("DELETE FROM analysis_results WHERE work_id = $1")
            .bind(work_id)
            .execute(&mut *tx)
            .await?;

        sqlx::query("INSERT INTO analysis_results (work_id, error_message) VALUES ($1, $2)")
            .bind(work_id)
            .bind(error_message)
            .execute(&mut *tx)
            .await?;

        tx.commit().await?;
        Ok(())
    }

    pub async fn clear(pool: &DbPool, work_id: Uuid) -> Result<(), sqlx::Error> {
        sqlx::query("DELETE FROM analysis_results WHERE work_id = $1")
            .bind(work_id)
            .execute(pool)
            .await?
            .rows_affected();
        Ok(())
    }

    pub async fn for_work(pool: &DbPool, work_id: Uuid) -> Result<Vec<AnalysisRow>, sqlx::Error> {
        sqlx::query_as::<_, (Option<Uuid>, Option<String>, bool)>(
            "SELECT stolen_from_id, error_message, is_running FROM analysis_results WHERE work_id = $1 ORDER BY created_at ASC",
        )
        .bind(work_id)
        .fetch_all(pool)
        .await
        .map(|rows| {
            rows
                .into_iter()
                .map(|(stolen_from_id, error_message, is_running)| AnalysisRow {
                    stolen_from_id,
                    error_message,
                    is_running,
                })
                .collect()
        })
    }
}
