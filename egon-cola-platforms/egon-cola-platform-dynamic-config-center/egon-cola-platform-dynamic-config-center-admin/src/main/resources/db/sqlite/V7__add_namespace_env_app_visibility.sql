alter table ddc_config_item add column biz_code varchar(128);
update ddc_config_item
   set biz_code = (
       select a.biz_code from ddc_app a
        where a.app_code = ddc_config_item.app_code
   );

-- Establish the physical uniqueness first so legacy conflicts abort V7.
create unique index uk_ddc_config_item_physical_guard
    on ddc_config_item(biz_code, env, app_code, config_key);

create table ddc_config_item_new (
    id varchar(64) primary key,
    biz_code varchar(128) not null,
    app_code varchar(128) not null,
    env varchar(32) not null,
    namespace varchar(128),
    config_key varchar(256) not null,
    config_value text,
    default_value text,
    value_type varchar(32) not null,
    current_version integer not null,
    published_version integer,
    description varchar(512),
    enabled integer not null default 1,
    deleted integer not null default 0,
    lock_version integer default 0,
    created_at datetime not null,
    updated_at datetime not null
);
insert into ddc_config_item_new (
    id, biz_code, app_code, env, namespace, config_key, config_value,
    default_value, value_type, current_version, published_version,
    description, enabled, deleted, lock_version, created_at, updated_at
)
select id, biz_code, app_code, env, namespace, config_key, config_value,
       default_value, value_type, current_version, published_version,
       description, enabled, deleted, lock_version, created_at, updated_at
  from ddc_config_item;
drop table ddc_config_item;
alter table ddc_config_item_new rename to ddc_config_item;
create unique index uk_ddc_config_item_physical
    on ddc_config_item(biz_code, env, app_code, config_key);

alter table ddc_config_version add column biz_code varchar(128);
update ddc_config_version
   set biz_code = (
       select a.biz_code from ddc_app a
        where a.app_code = ddc_config_version.app_code
   );
alter table ddc_publish_task add column biz_code varchar(128);
update ddc_publish_task
   set biz_code = (
       select a.biz_code from ddc_app a
        where a.app_code = ddc_publish_task.app_code
   );
alter table ddc_publish_ack add column biz_code varchar(128);
update ddc_publish_ack
   set biz_code = (
       select a.biz_code from ddc_app a
        where a.app_code = ddc_publish_ack.app_code
   );
alter table ddc_instance add column biz_code varchar(128);
update ddc_instance
   set biz_code = (
       select a.biz_code from ddc_app a
        where a.app_code = ddc_instance.app_code
   );
alter table ddc_operation_log add column biz_code varchar(128);
update ddc_operation_log
   set biz_code = (
       select a.biz_code from ddc_app a
        where a.app_code = ddc_operation_log.app_code
   );

alter table ddc_namespace add column biz_code varchar(128);
update ddc_namespace
   set biz_code = (
       select a.biz_code from ddc_app a
        where a.app_code = ddc_namespace.app_code
   );

create table ddc_namespace_env_app (
    id varchar(64) primary key,
    namespace_id varchar(64) not null,
    env_code varchar(32) not null,
    app_id varchar(64) not null,
    enabled integer not null default 1,
    created_at text not null,
    updated_at text not null
);
create unique index uk_ddc_namespace_env_app
    on ddc_namespace_env_app(namespace_id, env_code, app_id);

insert or ignore into ddc_namespace_env_app (
    id, namespace_id, env_code, app_id, enabled, created_at, updated_at
)
select lower(hex(randomblob(16))),
       (select min(n2.id)
          from ddc_namespace n2
          join ddc_app a2 on a2.app_code = n2.app_code
         where a2.biz_code = a.biz_code
           and n2.namespace_code = n.namespace_code),
       e.env_code,
       a.id,
       1,
       datetime('now'),
       datetime('now')
  from ddc_namespace n
  join ddc_app a on a.app_code = n.app_code
 cross join ddc_env e;

create table ddc_namespace_new (
    id varchar(64) primary key,
    biz_code varchar(128) not null,
    namespace_code varchar(128) not null,
    namespace varchar(128) not null,
    description varchar(512),
    enabled integer not null default 1,
    created_at text not null,
    updated_at text not null
);
insert into ddc_namespace_new (
    id, biz_code, namespace_code, namespace, description,
    enabled, created_at, updated_at
)
select n.id, n.biz_code, n.namespace_code, n.namespace, n.description,
       n.enabled, n.created_at, n.updated_at
  from ddc_namespace n
 where n.id = (
     select min(n2.id)
       from ddc_namespace n2
      where n2.biz_code = n.biz_code
        and n2.namespace_code = n.namespace_code
 );
drop table ddc_namespace;
alter table ddc_namespace_new rename to ddc_namespace;
create unique index uk_ddc_namespace_biz_code
    on ddc_namespace(biz_code, namespace_code);

drop index uk_ddc_app_code;
create unique index uk_ddc_app_biz_code
    on ddc_app(biz_code, app_code);
