alter table ddc_instance
    add column resource_server_id varchar(128);

alter table ddc_instance
    add column resource_version bigint;

alter table ddc_instance
    add column credential_id varchar(128);

alter table ddc_instance
    add column admission_expires_at timestamp;

create index idx_ddc_instance_resource_admission
    on ddc_instance(resource_server_id, resource_version, admission_expires_at);
