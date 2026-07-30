create table ddc_app (
    id varchar(64) primary key,
    app_code varchar(128) not null,
    app_name varchar(128) not null,
    owner varchar(128),
    description varchar(512),
    enabled boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table ddc_namespace (
    id varchar(64) primary key,
    app_code varchar(128) not null,
    env varchar(32) not null,
    namespace varchar(128) not null,
    description varchar(512),
    enabled boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table ddc_config_item (
    id varchar(64) primary key,
    app_code varchar(128) not null,
    env varchar(32) not null,
    namespace varchar(128) not null,
    config_key varchar(256) not null,
    config_value text,
    default_value text,
    value_type varchar(32) not null,
    current_version bigint not null,
    description varchar(512),
    enabled boolean not null default true,
    deleted boolean not null default false,
    lock_version bigint default 0,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table ddc_config_version (
    id varchar(64) primary key,
    config_id varchar(64) not null,
    app_code varchar(128) not null,
    env varchar(32) not null,
    namespace varchar(128) not null,
    config_key varchar(256) not null,
    version bigint not null,
    old_value text,
    new_value text,
    value_type varchar(32),
    change_type varchar(32),
    change_reason varchar(512),
    operator varchar(128),
    operator_ip varchar(64),
    created_at timestamp not null
);

create table ddc_publish_task (
    id varchar(64) primary key,
    change_id varchar(64) not null,
    config_id varchar(64),
    app_code varchar(128),
    env varchar(32),
    namespace varchar(128),
    config_key varchar(256),
    target_version bigint,
    publish_mode varchar(32),
    status varchar(32),
    target_count integer default 0,
    ack_count integer default 0,
    failed_count integer default 0,
    ignored_count integer default 0,
    timeout_count integer default 0,
    timeout_ms bigint,
    operator varchar(128),
    error_message text,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table ddc_publish_ack (
    id varchar(64) primary key,
    change_id varchar(64) not null,
    instance_id varchar(256) not null,
    app_code varchar(128),
    env varchar(32),
    namespace varchar(128),
    config_key varchar(256),
    target_version bigint,
    current_version bigint,
    ack_status varchar(32),
    error_message text,
    ack_at timestamp
);

create table ddc_instance (
    id varchar(64) primary key,
    instance_id varchar(256) not null,
    app_code varchar(128),
    env varchar(32),
    namespace varchar(128),
    host varchar(128),
    port integer,
    pid varchar(128),
    sdk_version varchar(64),
    status varchar(32),
    last_heartbeat_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table ddc_operation_log (
    id varchar(64) primary key,
    app_code varchar(128),
    env varchar(32),
    namespace varchar(128),
    config_key varchar(256),
    operation_type varchar(64),
    operator varchar(128),
    operator_ip varchar(64),
    operation_content text,
    created_at timestamp not null
);

create unique index uk_ddc_app_code on ddc_app(app_code);
create unique index uk_ddc_namespace_key on ddc_namespace(app_code, env, namespace);
create unique index uk_ddc_config_item_key on ddc_config_item(app_code, env, namespace, config_key);
create unique index uk_ddc_publish_task_change_id on ddc_publish_task(change_id);
create unique index uk_ddc_publish_ack_instance on ddc_publish_ack(change_id, instance_id);
create unique index uk_ddc_instance_id on ddc_instance(instance_id);
