alter table ddc_config_item add column biz_code varchar(128);
update ddc_config_item c
   set biz_code = a.biz_code
  from ddc_app a
 where a.app_code = c.app_code;
alter table ddc_config_item alter column biz_code set not null;

-- Fail the migration before removing the legacy constraint when two namespace
-- rows contain conflicting values for one physical config resource.
create unique index uk_ddc_config_item_physical
    on ddc_config_item(biz_code, env, app_code, config_key);
drop index uk_ddc_config_item_key;
alter table ddc_config_item alter column namespace drop not null;

alter table ddc_config_version add column biz_code varchar(128);
update ddc_config_version v
   set biz_code = a.biz_code
  from ddc_app a
 where a.app_code = v.app_code;
alter table ddc_config_version alter column namespace drop not null;

alter table ddc_publish_task add column biz_code varchar(128);
update ddc_publish_task t
   set biz_code = a.biz_code
  from ddc_app a
 where a.app_code = t.app_code;

alter table ddc_publish_ack add column biz_code varchar(128);
update ddc_publish_ack p
   set biz_code = a.biz_code
  from ddc_app a
 where a.app_code = p.app_code;

alter table ddc_instance add column biz_code varchar(128);
update ddc_instance i
   set biz_code = a.biz_code
  from ddc_app a
 where a.app_code = i.app_code;

alter table ddc_operation_log add column biz_code varchar(128);
update ddc_operation_log l
   set biz_code = a.biz_code
  from ddc_app a
 where a.app_code = l.app_code;

alter table ddc_namespace add column biz_code varchar(128);
update ddc_namespace n
   set biz_code = a.biz_code
  from ddc_app a
 where a.app_code = n.app_code;
alter table ddc_namespace alter column biz_code set not null;

create table ddc_namespace_env_app (
    id varchar(64) primary key,
    namespace_id varchar(64) not null references ddc_namespace(id),
    env_code varchar(32) not null references ddc_env(env_code),
    app_id varchar(64) not null references ddc_app(id),
    enabled boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);

create unique index uk_ddc_namespace_env_app
    on ddc_namespace_env_app(namespace_id, env_code, app_id);

insert into ddc_namespace_env_app (
    id, namespace_id, env_code, app_id, enabled, created_at, updated_at
)
select md5(n.id || ':' || e.id || ':' || a.id),
       (select min(n2.id)
          from ddc_namespace n2
          join ddc_app a2 on a2.app_code = n2.app_code
         where a2.biz_code = a.biz_code
           and n2.namespace_code = n.namespace_code),
       e.env_code,
       a.id,
       true,
       now(),
       now()
  from ddc_namespace n
  join ddc_app a on a.app_code = n.app_code
 cross join ddc_env e
on conflict (namespace_id, env_code, app_id) do nothing;

delete from ddc_namespace n
 where n.id <> (
     select min(n2.id)
       from ddc_namespace n2
      where n2.biz_code = n.biz_code
        and n2.namespace_code = n.namespace_code
 );

drop index uk_ddc_namespace_key;
drop index uk_ddc_namespace_code;
alter table ddc_namespace drop column app_code;
create unique index uk_ddc_namespace_biz_code
    on ddc_namespace(biz_code, namespace_code);

drop index uk_ddc_app_code;
create unique index uk_ddc_app_biz_code
    on ddc_app(biz_code, app_code);
