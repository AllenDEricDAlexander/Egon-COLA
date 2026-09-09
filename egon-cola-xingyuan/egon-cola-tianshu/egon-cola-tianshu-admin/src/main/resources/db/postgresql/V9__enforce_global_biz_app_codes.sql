drop index uk_ddc_app_biz_code;

create unique index uk_ddc_app_code
    on ddc_app(app_code);
