alter table ddc_namespace add column namespace_code varchar(128);
update ddc_namespace set namespace_code = namespace where namespace_code is null;
alter table ddc_namespace alter column namespace_code set not null;
create unique index uk_ddc_namespace_code on ddc_namespace(namespace_code);
