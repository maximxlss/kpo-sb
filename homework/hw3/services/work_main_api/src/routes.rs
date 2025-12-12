mod works;

pub fn configure(cfg: &mut actix_web::web::ServiceConfig) {
    cfg.service(works::create_work)
        .service(works::reanalyze_work)
        .service(works::get_report)
        .service(works::get_word_cloud);
}
