create table ddc_biz (
    id varchar(64) primary key,
    biz_code varchar(128) not null,
    biz_name varchar(128) not null,
    description varchar(512),
    enabled boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table ddc_env (
    id varchar(64) primary key,
    env_code varchar(32) not null,
    description varchar(256),
    sort_order int not null default 0,
    enabled boolean not null default true,
    created_at timestamp not null,
    updated_at timestamp not null
);

insert into ddc_biz (id, biz_code, biz_name, description, enabled, created_at, updated_at)
values ('biz-default', 'default', '默认业务域', 'V5 迁移自动创建', true, now(), now());

insert into ddc_env (id, env_code, description, sort_order, enabled, created_at, updated_at)
values ('env-dev', 'dev', '开发环境', 10, true, now(), now()),
       ('env-test', 'test', '测试环境', 20, true, now(), now()),
       ('env-sit', 'sit', '集成环境', 30, true, now(), now()),
       ('env-gray', 'gray', '灰度环境', 40, true, now(), now()),
       ('env-prod', 'prod', '生产环境', 50, true, now(), now());

alter table ddc_app add column biz_code varchar(128);
update ddc_app set biz_code = 'default' where biz_code is null;
alter table ddc_app alter column biz_code set not null;

-- ddc_namespace 去 env：按 (app_code, namespace) 去重后重建
delete from ddc_namespace
 where id not in (
     select min(id) from ddc_namespace group by app_code, namespace
 );
drop index uk_ddc_namespace_key;
alter table ddc_namespace drop column env;
create unique index uk_ddc_namespace_key on ddc_namespace(app_code, namespace);

create unique index uk_ddc_biz_code on ddc_biz(biz_code);
create unique index uk_ddc_env_code on ddc_env(env_code);
