use std::time::Duration;

use anyhow::{Result, anyhow};
use reqwest::{multipart, Client};
use reqwest::header::CONTENT_LENGTH;
use serde::Deserialize;
use tokio::time::sleep;
use uuid::Uuid;

const BASE_URL: &str = "http://localhost"; // public router
const POLL_INTERVAL: Duration = Duration::from_secs(1);
const POLL_TIMEOUT: Duration = Duration::from_secs(60);

#[derive(Deserialize)]
struct CreateWorkResponse {
    id: Uuid,
}

#[derive(Deserialize, Debug)]
#[serde(rename_all = "snake_case")]
enum ReportStatus {
    Running,
    Pending,
    Failed,
    Completed,
}

#[derive(Deserialize, Debug)]
struct ReportResponse {
    status: ReportStatus,
    #[allow(dead_code)]
    is_plagiarism: Option<bool>,
    #[allow(dead_code)]
    error: Option<String>,
    matches: Vec<Uuid>,
}

fn http_client() -> Client {
    Client::builder()
        .http1_only()
        .user_agent("integration-test/1.0")
        .build()
        .expect("reqwest client")
}

#[tokio::main]
async fn main() -> Result<()> {
    eprintln!("starting integration flow against {BASE_URL}");

    let client = http_client();
    let run_tag = Uuid::new_v4();
    let short_tag = run_tag.simple().to_string();

    let sample_text = format!(
        "TEST MATERIAL ONLY; integration flow positive case; run={short_tag}; payload=plagiarism control; line=1\nTEST MATERIAL ONLY; run={short_tag}; line=2"
    )
    .into_bytes();
    let sample_diff = format!(
        "TEST MATERIAL ONLY; integration flow negative case; run={short_tag}; payload=different content; line=1\nThis line intentionally differs for run={short_tag}"
    )
    .into_bytes();

    let assignment_name = format!("test-assignment-{short_tag}");
    let comment = format!("integration test run={short_tag}");
    let author_a = format!("test-author-alice-{short_tag}");
    let author_b = format!("test-author-bob-{short_tag}");
    let author_c = format!("test-author-carol-{short_tag}");

    // 1) Create first work (author alice)
    eprintln!("creating work A (author={author_a})");
    let work_a = create_work(&client, &author_a, &assignment_name, &comment, &sample_text).await?;
    eprintln!("created work A: {work_a}");
    let report_a = poll_report(&client, work_a).await?;
    ensure_completed(&report_a, false, Some(&[]))?;

    // 2) Create second work with same content (author bob) to trigger plagiarism
    eprintln!("creating work B (author={author_b})");
    let work_b = create_work(&client, &author_b, &assignment_name, &comment, &sample_text).await?;
    eprintln!("created work B: {work_b}");
    let report_b = poll_report(&client, work_b).await?;
    ensure_completed(&report_b, true, Some(&[work_a]))?;
    if !report_b.matches.contains(&work_a) {
        return Err(anyhow!("expected matches to contain first work id"));
    }

    // 3) Reanalyze B and ensure it settles again
    eprintln!("triggering reanalyze for work B");
    trigger_reanalyze(&client, work_b).await?;
    let report_b_again = poll_report(&client, work_b).await?;
    ensure_completed(&report_b_again, true, Some(&[work_a]))?;

    // 3b) Create third work with different content (author carol) and ensure no plagiarism
    eprintln!("creating work C (author={author_c})");
    let work_c = create_work(&client, &author_c, &assignment_name, &comment, &sample_diff).await?;
    eprintln!("created work C: {work_c}");
    let report_c = poll_report(&client, work_c).await?;
    ensure_completed(&report_c, false, Some(&[]))?;

    // 4) Download work B to validate public download path
    eprintln!("downloading work B: {work_b}");
    download_work(&client, work_b).await?;

    eprintln!("integration flow succeeded");
    Ok(())
}

async fn create_work(client: &Client, author: &str, assignment: &str, comment: &str, content: &[u8]) -> Result<Uuid> {
    let part = multipart::Part::bytes(content.to_vec())
        .file_name("work.txt")
        .mime_str("text/plain")?;

    let form = multipart::Form::new()
        .part("file", part)
        .text("author_name", author.to_string())
        .text("assignment_name", assignment.to_string())
        .text("comment", comment.to_string());

    let resp = client
        .post(format!("{BASE_URL}/works"))
        .multipart(form)
        .send()
        .await?
        .error_for_status()?;

    let body: CreateWorkResponse = resp.json().await?;
    Ok(body.id)
}

async fn poll_report(client: &Client, work_id: Uuid) -> Result<ReportResponse> {
    let start = tokio::time::Instant::now();

    loop {
        let resp = client
            .get(format!("{BASE_URL}/works/{work_id}/report"))
            .send()
            .await?;

        let status = resp.status();
        if status == reqwest::StatusCode::ACCEPTED {
            let body: ReportResponse = resp.json().await?;
            eprintln!(
                "report {work_id}: status={:?} elapsed={}s",
                body.status,
                start.elapsed().as_secs()
            );
            match body.status {
                ReportStatus::Running | ReportStatus::Pending => {
                    if start.elapsed() > POLL_TIMEOUT {
                        return Err(anyhow!("report did not complete in time"));
                    }
                    sleep(POLL_INTERVAL).await;
                    continue;
                }
                ReportStatus::Failed => {
                    return Err(anyhow!("analysis failed while pending: {:?}", body.error));
                }
                ReportStatus::Completed => {
                    eprintln!(
                        "report {work_id} completed: is_plagiarism={:?} matches={} entries",
                        body.is_plagiarism,
                        body.matches.len()
                    );
                    return Ok(body);
                }
            }
        } else if status.is_success() {
            let body: ReportResponse = resp.json().await?;
            eprintln!(
                "report {work_id} returned final status {:?} without polling",
                body.status
            );
            return Ok(body);
        } else {
            return Err(anyhow!("unexpected status {}", status));
        }
    }
}

async fn trigger_reanalyze(client: &Client, work_id: Uuid) -> Result<()> {
    let resp = client
        .post(format!("{BASE_URL}/works/{work_id}/reanalyze"))
        .header(CONTENT_LENGTH, 0)
        .send()
        .await?
        .error_for_status()?;
    if resp.status() != reqwest::StatusCode::ACCEPTED {
        return Err(anyhow!("reanalyze did not return 202"));
    }
    Ok(())
}

async fn download_work(client: &Client, work_id: Uuid) -> Result<()> {
    let resp = client
        .get(format!("{BASE_URL}/works/{work_id}/download"))
        .send()
        .await?
        .error_for_status()?;
    let ctype = resp
        .headers()
        .get(reqwest::header::CONTENT_TYPE)
        .and_then(|h| h.to_str().ok())
        .unwrap_or("")
        .to_string();
    if !ctype.starts_with("application/octet-stream") && !ctype.starts_with("text/") {
        // file_storage sets attachment; allow both
        eprintln!("warning: unexpected content-type: {ctype}");
    }
    let _bytes = resp.bytes().await?;
    eprintln!("download succeeded for work {work_id} (content-type={ctype})");
    Ok(())
}

fn ensure_completed(
    report: &ReportResponse,
    expect_plagiarism: bool,
    expected_matches: Option<&[Uuid]>,
) -> Result<()> {
    match report.status {
        ReportStatus::Completed => {
            let plag = report.is_plagiarism.unwrap_or(false);
            if plag != expect_plagiarism {
                return Err(anyhow!(
                    "unexpected plagiarism flag: got {}, expected {}",
                    plag,
                    expect_plagiarism
                ));
            }
            if let Some(expected) = expected_matches {
                let mut want: Vec<Uuid> = expected.to_vec();
                let mut got = report.matches.clone();
                want.sort();
                got.sort();
                if got != want {
                    return Err(anyhow!(
                        "unexpected matches: got {:?}, expected {:?}",
                        got,
                        want
                    ));
                }
            }
            Ok(())
        }
        ref s => Err(anyhow!("report not completed: {s:?}")),
    }
}
