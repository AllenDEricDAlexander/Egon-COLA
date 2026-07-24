alter table ddc_instance
    add column lease_id varchar(64);

alter table ddc_instance
    add column lease_expire_at timestamp;

alter table ddc_publish_task
    add column content_checksum varchar(64);

alter table ddc_publish_task
    add column attempt_count integer not null default 0;

alter table ddc_publish_task
    add column dispatched_at timestamp;

alter table ddc_publish_task
    add column completed_at timestamp;

alter table ddc_publish_task
    add column failure_stage varchar(64);

alter table ddc_publish_ack
    add column lease_id varchar(64);

alter table ddc_publish_ack
    add column content_checksum varchar(64);

drop index uk_ddc_publish_ack_instance;

create unique index uk_ddc_publish_ack_target
    on ddc_publish_ack(change_id, instance_id, lease_id);
