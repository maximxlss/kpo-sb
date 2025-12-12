use actix_web::{HttpResponse, Responder, get, web};

#[get("/healthcheck")]
async fn healthcheck() -> impl Responder {
    HttpResponse::Ok().body("Healthy")
}

pub fn configure(cfg: &mut web::ServiceConfig) {
    cfg.service(healthcheck);
}
