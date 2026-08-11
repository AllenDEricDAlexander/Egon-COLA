package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcV8MigrationTest {

    @Test
    void sqliteV8AddsOnlyAdmissionAuditColumnsAndIndex() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            execute(connection, """
                    create table ddc_instance (
                        id varchar(64) primary key,
                        instance_id varchar(256) not null
                    )
                    """);

            execute(connection, script(
                    "db/sqlite/V8__add_resource_admission_audit.sql"
            ));

            assertThat(tableColumns(connection, "ddc_instance"))
                    .contains(
                            "resource_server_id",
                            "resource_version",
                            "credential_id",
                            "admission_expires_at"
                    );
            assertThat(indexColumns(
                    connection,
                    "idx_ddc_instance_resource_admission"
            )).containsExactly(
                    "resource_server_id",
                    "resource_version",
                    "admission_expires_at"
            );
        }
    }

    @Test
    void postgresqlV8MatchesTheSameLogicalAuditMigration() throws Exception {
        String sql = script(
                "db/postgresql/V8__add_resource_admission_audit.sql"
        ).toLowerCase();

        assertThat(sql)
                .contains("add column resource_server_id varchar(128)")
                .contains("add column resource_version bigint")
                .contains("add column credential_id varchar(128)")
                .contains("add column admission_expires_at timestamp")
                .contains("idx_ddc_instance_resource_admission")
                .doesNotContain("admission_ticket")
                .doesNotContain("assertion");
    }

    private List<String> tableColumns(
            Connection connection,
            String table
    ) throws Exception {
        List<String> columns = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "pragma table_info('" + table + "')")) {
            while (result.next()) {
                columns.add(result.getString("name"));
            }
        }
        return columns;
    }

    private List<String> indexColumns(
            Connection connection,
            String index
    ) throws Exception {
        List<String> columns = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "pragma index_info('" + index + "')")) {
            while (result.next()) {
                columns.add(result.getString("name"));
            }
        }
        return columns;
    }

    private void execute(Connection connection, String sql) throws Exception {
        for (String statementSql : sql.split(";")) {
            if (!statementSql.isBlank()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(statementSql);
                }
            }
        }
    }

    private String script(String path) throws Exception {
        try (var input = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("missing migration: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
