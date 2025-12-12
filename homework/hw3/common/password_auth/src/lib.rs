use std::sync::LazyLock;

#[doc(hidden)]
pub use actix_web;
#[doc(hidden)]
pub use actix_web_httpauth;

use anyhow::Context as _;

pub const PASSWORD_ENV_VAR: &str = "ACCESS_PASSWORD";
pub static PASSWORD: LazyLock<anyhow::Result<String>> =
    LazyLock::new(|| std::env::var(PASSWORD_ENV_VAR).context(format!("env {PASSWORD_ENV_VAR}")));

pub fn check_for_env() -> anyhow::Result<()> {
    if let Err(err) = PASSWORD.as_ref() {
        return Err(anyhow::anyhow!("{err:#}"));
    }
    Ok(())
}

#[macro_export]
macro_rules! auth_middleware {
    () => {{
        use std::ops::Deref as _;
        use $crate::actix_web::dev::ServiceRequest;
        use $crate::actix_web_httpauth::{
            extractors::bearer::BearerAuth, middleware::HttpAuthentication,
        };
        HttpAuthentication::bearer(|req: ServiceRequest, credentials: BearerAuth| async move {
            let password = match $crate::PASSWORD.deref() {
                Ok(p) => p,
                Err(err) => {
                    return Err(($crate::actix_web::error::ErrorInternalServerError(err), req));
                }
            };

            if credentials.token() == password {
                Ok(req)
            } else {
                Err((
                    $crate::actix_web::error::ErrorUnauthorized("Invalid password"),
                    req,
                ))
            }
        })
    }};
}
