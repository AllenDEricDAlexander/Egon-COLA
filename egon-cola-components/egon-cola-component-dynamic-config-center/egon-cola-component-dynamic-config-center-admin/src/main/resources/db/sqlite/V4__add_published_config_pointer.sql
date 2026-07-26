alter table ddc_config_item add column published_version integer;
update ddc_config_item set published_version = current_version;
