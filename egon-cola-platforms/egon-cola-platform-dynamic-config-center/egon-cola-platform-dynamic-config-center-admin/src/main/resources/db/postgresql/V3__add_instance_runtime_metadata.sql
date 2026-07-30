alter table ddc_instance
    add column runtime_metadata text not null default '{}';
