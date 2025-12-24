FROM rust:latest AS builder

WORKDIR /app

COPY orders-service /app/orders-service
COPY payments-service /app/payments-service
COPY integration_test /app/integration_test
COPY Cargo.toml Cargo.lock /app/

RUN --mount=type=cache,target=/app/target/ \
    --mount=type=cache,target=/usr/local/cargo/git/db \
    --mount=type=cache,target=/usr/local/cargo/registry/ \
    cargo build --release && cp -r /app/target/release /artifacts

FROM debian:trixie-slim AS runtime
WORKDIR /app
CMD []

FROM runtime AS orders-service
COPY --from=builder /artifacts/orders-service /app/orders-service
EXPOSE 8081
CMD ["./orders-service"]

FROM runtime AS payments-service
COPY --from=builder /artifacts/payments-service /app/payments-service
EXPOSE 8082
CMD ["./payments-service"]
