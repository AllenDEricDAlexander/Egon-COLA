create table ddc_biz (
    id varchar(64) primary key,
    biz_code varchar(128) not null,
    biz_name varchar(128) not null,
    description varchar(512),
    enabled integer not null default 1,
    created_at text not null,
    updated_at text not null
);

create table ddc_env (
    id varchar(64) primary key,
    env_code varchar(32) not null,
    description varchar(256),
    sort_order integer not null default 0,
    enabled integer not null default 1,
    created_at text not null,
    updated_at text not null
);

insert into ddc_biz (id, biz_code, biz_name, description, enabled, created_at, updated_at)
values ('biz-default', 'default', '默认业务域', 'V5 迁移自动创建', 1, datetime('now'), datetime('now'));

insert into ddc_env (id, env_code, description, sort_order, enabled, created_at, updated_at)
values ('env-dev', 'dev', '开发环境', 10, 1, datetime('now'), datetime('now')),
       ('env-test', 'test', '测试环境', 20, 1, datetime('now'), datetime('now')),
       ('env-sit', 'sit', '集成环境', 30, 1, datetime('now'), datetime('now')),
       ('env-gray', 'gray', '灰度环境', 40, 1, datetime('now'), datetime('now')),
       ('env-prod', 'prod', '生产环境', 50, 1, datetime('now'), datetime('now'));

alter table ddc_app add column biz_code varchar(128);
update ddc_app set biz_code = 'default' where biz_code is null;

-- sqlite 删列需重建表
create table ddc_namespace_new (
    id varchar(64) primary key,
    app_code varchar(128) not null,
    namespace varchar(128) not null,
    description varchar(512),
    enabled integer not null default 1,
    created_at text not null,
    updated_at text not null
);
insert into ddc_namespace_new (id, app_code, namespace, description, enabled, created_at, updated_at)
select id, app_code, namespace, description, enabled, created_at, updated_at
  from ddc_namespace
 where id in (select min(id) from ddc_namespace group by app_code, namespace);
drop table ddc_namespace;
alter table ddc_namespace_new rename to ddc_namespace;

create unique index uk_ddc_namespace_key on ddc_namespace(app_code, namespace);
create unique index uk_ddc_biz_code on ddc_biz(biz_code);
create unique index uk_ddc_env_code on ddc_env(env_code);
