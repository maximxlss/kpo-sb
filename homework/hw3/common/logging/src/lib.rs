use std::sync::OnceLock;

static INIT: OnceLock<()> = OnceLock::new();

pub fn init() {
    INIT.get_or_init(|| {
        let env_filter = tracing_subscriber::EnvFilter::try_from_default_env()
            .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("info"));
        let _ = tracing_subscriber::fmt()
            .with_env_filter(env_filter)
            .with_target(false)
            .try_init();
    });
}

use tracing_actix_web::{DefaultRootSpanBuilder, TracingLogger};

pub fn logger() -> TracingLogger<DefaultRootSpanBuilder> {
    TracingLogger::new()
}
