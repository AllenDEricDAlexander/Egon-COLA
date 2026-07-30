package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DdcSchemaScriptTest {

    @Test
    void postgresqlScriptContainsRequiredTables() throws Exception {
        String sql = script("db/postgresql/V1__create_ddc_schema.sql");
        assertThat(sql).contains("create table ddc_app");
        assertThat(sql).contains("create table ddc_namespace");
        assertThat(sql).contains("create table ddc_config_item");
        assertThat(sql).contains("create table ddc_config_version");
        assertThat(sql).contains("create table ddc_publish_task");
        assertThat(sql).contains("create table ddc_publish_ack");
        assertThat(sql).contains("create table ddc_instance");
        assertThat(sql).contains("create table ddc_operation_log");
        assertThat(sql).doesNotContain("lease_id");

        String migration = script("db/postgresql/V2__add_lease_and_sync_publish.sql");
        assertV2(migration);
    }

    @Test
    void sqliteScriptContainsRequiredTables() throws Exception {
        String sql = script("db/sqlite/V1__create_ddc_schema.sql");
        assertThat(sql).contains("create table ddc_app");
        assertThat(sql).contains("create table ddc_namespace");
        assertThat(sql).contains("create table ddc_config_item");
        assertThat(sql).contains("create table ddc_config_version");
        assertThat(sql).contains("create table ddc_publish_task");
        assertThat(sql).contains("create table ddc_publish_ack");
        assertThat(sql).contains("create table ddc_instance");
        assertThat(sql).contains("create table ddc_operation_log");
        assertThat(sql).doesNotContain("lease_id");

        String migration = script("db/sqlite/V2__add_lease_and_sync_publish.sql");
        assertV2(migration);
    }

    private void assertV2(String sql) {
        assertThat(sql).contains("lease_id");
        assertThat(sql).contains("lease_expire_at");
        assertThat(sql).contains("content_checksum");
        assertThat(sql).contains("attempt_count");
        assertThat(sql).contains("dispatched_at");
        assertThat(sql).contains("completed_at");
        assertThat(sql).contains("failure_stage");
        assertThat(sql).contains("drop index uk_ddc_publish_ack_instance");
        assertThat(sql).contains("uk_ddc_publish_ack_target");
    }

    private String script(String path) throws Exception {
        try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }
    }
}
