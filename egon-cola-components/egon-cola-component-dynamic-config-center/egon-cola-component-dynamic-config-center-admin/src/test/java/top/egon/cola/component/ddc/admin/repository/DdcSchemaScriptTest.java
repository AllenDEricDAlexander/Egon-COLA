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
    }

    private String script(String path) throws Exception {
        try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }
    }
}
