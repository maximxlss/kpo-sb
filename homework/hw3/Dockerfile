FROM rust:latest AS builder

WORKDIR /app

COPY common /app/common
COPY services /app/services
COPY integration_test /app/integration_test
COPY Cargo.toml Cargo.lock /app/

RUN --mount=type=cache,target=/app/target/ \
    --mount=type=cache,target=/usr/local/cargo/git/db \
    --mount=type=cache,target=/usr/local/cargo/registry/ \
    cargo build --release && cp -r /app/target/release /artifacts

FROM debian:trixie-slim AS base_image
WORKDIR /app
COPY --from=builder /artifacts/check_health /usr/local/bin/check_health
HEALTHCHECK --interval=3s --retries=3 CMD check_health
EXPOSE 8080
CMD []

FROM base_image AS file_storage
COPY --from=builder /artifacts/file_storage /usr/local/bin/file_storage
CMD ["file_storage"]

FROM base_image AS work_analyzer
COPY --from=builder /artifacts/work_analyzer /usr/local/bin/work_analyzer
CMD ["work_analyzer"]

FROM base_image AS work_main_api
COPY --from=builder /artifacts/work_main_api /usr/local/bin/work_main_api
CMD ["work_main_api"]
