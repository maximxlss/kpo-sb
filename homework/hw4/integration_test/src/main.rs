use anyhow::{anyhow, Context, Result};
use reqwest::{Client, StatusCode};
use serde_json::{json, Value};
use std::env;
use std::time::{Duration, Instant};
use uuid::Uuid;

#[tokio::main]
async fn main() -> Result<()> {
    let base_url = env::var("E2E_BASE_URL").unwrap_or_else(|_| "http://localhost".to_string());
    let client = Client::builder()
        .timeout(Duration::from_secs(5))
        .build()
        .context("build http client")?;

    println!("using base url: {base_url}");

    health_checks(&client, &base_url).await?;
    account_paths(&client, &base_url).await?;
    order_paths(&client, &base_url).await?;

    println!("integration test completed");
    Ok(())
}

async fn health_checks(client: &Client, base_url: &str) -> Result<()> {
    let resp = client
        .get(format!("{base_url}/health"))
        .send()
        .await
        .context("health request")?;
    if resp.status() != StatusCode::OK {
        return Err(anyhow!("health check failed: {}", resp.status()));
    }
    Ok(())
}

async fn account_paths(client: &Client, base_url: &str) -> Result<()> {
    let user_id = Uuid::new_v4();

    let create = client
        .post(format!("{base_url}/accounts"))
        .json(&json!({ "user_id": user_id }))
        .send()
        .await
        .context("create account request")?;
    ensure_success(create.status(), "create account")?;
    let account: Value = create.json().await.context("create account json")?;
    ensure_i64(&account, "balance_cents", 0)?;

    let create_again = client
        .post(format!("{base_url}/accounts"))
        .json(&json!({ "user_id": user_id }))
        .send()
        .await
        .context("create account conflict request")?;
    if create_again.status() != StatusCode::CONFLICT {
        return Err(anyhow!(
            "expected account conflict, got {}",
            create_again.status()
        ));
    }

    let topup = client
        .post(format!("{base_url}/accounts/{user_id}/topup"))
        .json(&json!({ "amount_cents": 5000 }))
        .send()
        .await
        .context("topup request")?;
    ensure_success(topup.status(), "topup")?;
    let topup_account: Value = topup.json().await.context("topup json")?;
    ensure_i64(&topup_account, "balance_cents", 5000)?;

    let missing_user = Uuid::new_v4();
    let missing_topup = client
        .post(format!("{base_url}/accounts/{missing_user}/topup"))
        .json(&json!({ "amount_cents": 1000 }))
        .send()
        .await
        .context("topup missing request")?;
    if missing_topup.status() != StatusCode::NOT_FOUND {
        return Err(anyhow!(
            "expected topup missing 404, got {}",
            missing_topup.status()
        ));
    }

    let get_account = client
        .get(format!("{base_url}/accounts/{user_id}"))
        .send()
        .await
        .context("get account request")?;
    ensure_success(get_account.status(), "get account")?;
    let get_account_body: Value = get_account.json().await.context("get account json")?;
    ensure_i64(&get_account_body, "balance_cents", 5000)?;

    let get_missing = client
        .get(format!("{base_url}/accounts/{missing_user}"))
        .send()
        .await
        .context("get account missing request")?;
    if get_missing.status() != StatusCode::NOT_FOUND {
        return Err(anyhow!(
            "expected get account missing 404, got {}",
            get_missing.status()
        ));
    }

    Ok(())
}

async fn order_paths(client: &Client, base_url: &str) -> Result<()> {
    let user_id = Uuid::new_v4();

    create_and_topup(client, base_url, user_id, 5000).await?;
    let first_description = "test order 1";
    let (order_id, status) =
        create_order(client, base_url, user_id, 2000, first_description).await?;
    if status != "NEW" {
        return Err(anyhow!("expected NEW status on create, got {status}"));
    }

    let final_status = wait_for_order_status(client, base_url, user_id, &order_id).await?;
    if final_status != "FINISHED" {
        return Err(anyhow!("expected FINISHED status, got {final_status}"));
    }

    let account = get_account(client, base_url, user_id).await?;
    ensure_i64(&account, "balance_cents", 3000)?;

    let order = get_order(client, base_url, user_id, &order_id).await?;
    ensure_string(&order, "description", first_description)?;

    let list = list_orders(client, base_url, user_id).await?;
    ensure_order_list_contains(&list, &order_id)?;
    ensure_order_list_description(&list, &order_id, first_description)?;

    let second_description = "test order 2";
    let (order_id_low, _) =
        create_order(client, base_url, user_id, 4000, second_description).await?;
    let status_low = wait_for_order_status(client, base_url, user_id, &order_id_low).await?;
    if status_low != "CANCELED" {
        return Err(anyhow!(
            "expected CANCELED status due to low balance, got {status_low}"
        ));
    }

    let order_low = get_order(client, base_url, user_id, &order_id_low).await?;
    ensure_string(&order_low, "description", second_description)?;

    Ok(())
}

async fn create_and_topup(
    client: &Client,
    base_url: &str,
    user_id: Uuid,
    amount: i64,
) -> Result<()> {
    let create = client
        .post(format!("{base_url}/accounts"))
        .json(&json!({ "user_id": user_id }))
        .send()
        .await
        .context("create account for order")?;
    ensure_success(create.status(), "create account for order")?;

    let topup = client
        .post(format!("{base_url}/accounts/{user_id}/topup"))
        .json(&json!({ "amount_cents": amount }))
        .send()
        .await
        .context("topup for order")?;
    ensure_success(topup.status(), "topup for order")?;
    Ok(())
}

async fn create_order(
    client: &Client,
    base_url: &str,
    user_id: Uuid,
    amount: i64,
    description: &str,
) -> Result<(String, String)> {
    let order_resp = client
        .post(format!("{base_url}/orders"))
        .json(&json!({
            "user_id": user_id,
            "amount_cents": amount,
            "description": description
        }))
        .send()
        .await
        .context("create order request")?;
    ensure_success(order_resp.status(), "create order")?;

    let order_body: Value = order_resp.json().await.context("order json")?;
    let order_id = order_body["order_id"]
        .as_str()
        .ok_or_else(|| anyhow!("missing order_id"))?
        .to_string();
    let status = order_body["status"]
        .as_str()
        .ok_or_else(|| anyhow!("missing order status"))?
        .to_string();
    Ok((order_id, status))
}

async fn wait_for_order_status(
    client: &Client,
    base_url: &str,
    user_id: Uuid,
    order_id: &str,
) -> Result<String> {
    let deadline = Instant::now() + Duration::from_secs(10);
    while Instant::now() < deadline {
        let resp = client
            .get(format!("{base_url}/orders/{order_id}?user_id={user_id}"))
            .send()
            .await
            .context("get order status request")?;
        if resp.status() == StatusCode::OK {
            let body: Value = resp.json().await.context("order status json")?;
            if let Some(status) = body["status"].as_str() {
                if status != "NEW" {
                    return Ok(status.to_string());
                }
            }
        }
        tokio::time::sleep(Duration::from_millis(500)).await;
    }
    Err(anyhow!("order status did not settle in time"))
}

async fn get_order(
    client: &Client,
    base_url: &str,
    user_id: Uuid,
    order_id: &str,
) -> Result<Value> {
    let resp = client
        .get(format!("{base_url}/orders/{order_id}?user_id={user_id}"))
        .send()
        .await
        .context("get order request")?;
    ensure_success(resp.status(), "get order")?;
    resp.json().await.context("get order json")
}

async fn get_account(client: &Client, base_url: &str, user_id: Uuid) -> Result<Value> {
    let resp = client
        .get(format!("{base_url}/accounts/{user_id}"))
        .send()
        .await
        .context("get account request")?;
    ensure_success(resp.status(), "get account")?;
    resp.json().await.context("account json")
}

async fn list_orders(client: &Client, base_url: &str, user_id: Uuid) -> Result<Value> {
    let resp = client
        .get(format!("{base_url}/orders?user_id={user_id}"))
        .send()
        .await
        .context("list orders request")?;
    ensure_success(resp.status(), "list orders")?;
    resp.json().await.context("list json")
}

fn ensure_order_list_contains(list_body: &Value, order_id: &str) -> Result<()> {
    let list = list_body
        .as_array()
        .ok_or_else(|| anyhow!("orders list is not an array"))?;
    let contains = list
        .iter()
        .any(|item| item.get("order_id").and_then(|v| v.as_str()) == Some(order_id));
    if !contains {
        return Err(anyhow!("order {order_id} missing in list"));
    }
    Ok(())
}

fn ensure_order_list_description(list_body: &Value, order_id: &str, description: &str) -> Result<()> {
    let list = list_body
        .as_array()
        .ok_or_else(|| anyhow!("orders list is not an array"))?;
    let matches = list.iter().any(|item| {
        item.get("order_id").and_then(|v| v.as_str()) == Some(order_id)
            && item.get("description").and_then(|v| v.as_str()) == Some(description)
    });
    if !matches {
        return Err(anyhow!(
            "order {order_id} missing expected description in list"
        ));
    }
    Ok(())
}

fn ensure_i64(body: &Value, key: &str, expected: i64) -> Result<()> {
    let value = body
        .get(key)
        .and_then(|v| v.as_i64())
        .ok_or_else(|| anyhow!("missing {key}"))?;
    if value != expected {
        return Err(anyhow!("expected {key}={expected}, got {value}"));
    }
    Ok(())
}

fn ensure_string(body: &Value, key: &str, expected: &str) -> Result<()> {
    let value = body
        .get(key)
        .and_then(|v| v.as_str())
        .ok_or_else(|| anyhow!("missing {key}"))?;
    if value != expected {
        return Err(anyhow!("expected {key}={expected}, got {value}"));
    }
    Ok(())
}

fn ensure_success(status: StatusCode, label: &str) -> Result<()> {
    if !status.is_success() {
        return Err(anyhow!("{label} failed: {status}"));
    }
    Ok(())
}
