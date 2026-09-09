package top.egon.cola.component.yuheng.admin.release.repository.jdbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import top.egon.cola.component.yuheng.admin.release.domain.dto.GatewayPublicationScopeDTO;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayPublicationPhaseEnum;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayPublicationStatusEnum;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleasePublicationPO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetPO;
import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 中文说明：使用独占随机 schema 验证双角色发布迁移及真实 JDBC 读写，不启动应用。
 * English summary: Verifies migration and JDBC persistence in an owned random schema without starting an app.
 */
@EnabledIfEnvironmentVariable(named = "YUHENG_IT_POSTGRES_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "YUHENG_IT_POSTGRES_USER", matches = ".+")
@EnabledIfEnvironmentVariable(named = "YUHENG_IT_POSTGRES_PASSWORD_FILE", matches = ".+")
class GatewayPublicationTargetsPostgresqlIT {

    @Test
    void upgradesWithoutInventingHistoricalTargetsAndPersistsIndependentRoleVersions() throws Exception {
        String url = System.getenv("YUHENG_IT_POSTGRES_URL");
        String user = System.getenv("YUHENG_IT_POSTGRES_USER");
        String password = Files.readString(Path.of(System.getenv("YUHENG_IT_POSTGRES_PASSWORD_FILE"))).trim();
        String schema = "gateway_targets_it_" + UUID.randomUUID().toString().replace("-", "");
        assertThat(schema).matches("gateway_targets_it_[a-z0-9]+");
        boolean created = false;
        try (var connection = DriverManager.getConnection(url, user, password)) {
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.execute("CREATE SCHEMA " + schema);
            created = true;
            Flyway.configure().dataSource(url, user, password).defaultSchema(schema).schemas(schema)
                    .locations("classpath:db/migration").target("12").load().migrate();
            jdbc.execute("SET search_path TO " + schema);
            seedRelease(jdbc);
            jdbc.update("""
                    INSERT INTO gateway_release_publication (
                        release_id, attempt_no, phase_order, phase_type, config_key, content_value,
                        content_sha256, change_id, ddc_status, created_at, updated_at
                    ) VALUES ('release-1', 1, 0, 'ACTIVATION', 'yuheng.rules.active', '{}',
                              repeat('a', 64), 'historical', 'PLANNED', now(), now())
                    """);

            Flyway flyway = Flyway.configure().dataSource(url, user, password).defaultSchema(schema).schemas(schema)
                    .locations("classpath:db/migration").target("13").load();
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
            assertThat(flyway.migrate().migrationsExecuted).isZero();
            assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
            var journal = new JdbcGatewayReleasePublicationRepository(jdbc);
            assertThat(journal.findAttempt("release-1", 1)).singleElement()
                    .satisfies(phase -> assertThat(phase.targetScope()).isNull());

            Instant now = Instant.parse("2026-09-06T04:00:00Z");
            var api = scope(GatewayEngineRoleEnum.API_RPC);
            var mcp = scope(GatewayEngineRoleEnum.MCP);
            journal.insertAll(List.of(phase(api, 1, now), phase(mcp, 2, now)));
            for (var target : List.of(api, mcp)) {
                String changeId = target.engineRole().name();
                long version = target.engineRole() == GatewayEngineRoleEnum.API_RPC ? 3L : 40L;
                journal.resolveDocument(changeId, version, "yuheng: {rules: {active: '{}'}}", now);
                journal.markSubmitted(changeId, now);
                journal.markResult(changeId, version + 1, GatewayPublicationStatusEnum.FAILED,
                        "retry", "retry", now);
                journal.markResult(changeId, version + 1, GatewayPublicationStatusEnum.SUCCESS, null, null, now);
            }
            assertThat(journal.findOperation("release-1", 1, 1).orElseThrow().targetScope()).isEqualTo(api);
            assertThat(journal.findAttemptMetadata("release-1", 1).subList(1, 3))
                    .extracting(GatewayReleasePublicationPO::targetScope).containsExactly(api, mcp);
            assertThat(journal.findAttemptMetadata("release-1", 1).subList(1, 3))
                    .extracting(GatewayReleasePublicationPO::ddcTargetVersion).containsExactly(4L, 41L);
            assertThat(journal.findAttemptMetadata("release-1", 1))
                    .allSatisfy(phase -> assertThat(phase.contentValue()).isNull());
            assertThatThrownBy(() -> jdbc.update("""
                    UPDATE gateway_release_publication SET target_app_code = ''
                     WHERE change_id = 'MCP'
                    """)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThatThrownBy(() -> journal.insertAll(List.of(phase(api, 3, now))))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

            var releases = new JdbcGatewayReleaseRepository(jdbc, new ObjectMapper());
            releases.completeAttempt("release-1", 1, GatewayReleaseStatus.SUCCESS, false, "MCP", null, null,
                    List.of(new GatewayReleaseTargetPO("api", "api-lease", "SUCCESS", 4L, "sha",
                                    null, now, GatewayEngineRoleEnum.API_RPC),
                            new GatewayReleaseTargetPO("mcp", "mcp-lease", "SUCCESS", 41L, "sha",
                                    null, now, GatewayEngineRoleEnum.MCP)), now);
            assertThat(releases.attempts("release-1").getFirst().targets())
                    .extracting(GatewayReleaseTargetPO::engineRole)
                    .containsExactly(GatewayEngineRoleEnum.API_RPC, GatewayEngineRoleEnum.MCP);
        } finally {
            if (created && schema.matches("gateway_targets_it_[a-z0-9]+")) {
                try (var connection = DriverManager.getConnection(url, user, password);
                     var statement = connection.createStatement()) {
                    statement.execute("DROP SCHEMA " + schema + " CASCADE");
                }
            }
        }
    }

    private GatewayPublicationScopeDTO scope(GatewayEngineRoleEnum role) {
        return new GatewayPublicationScopeDTO("infra", "test",
                role == GatewayEngineRoleEnum.API_RPC ? "ge" : "gme", role);
    }

    private GatewayReleasePublicationPO phase(GatewayPublicationScopeDTO scope, int order, Instant now) {
        return new GatewayReleasePublicationPO("release-1", 1, order, GatewayPublicationPhaseEnum.ACTIVATION,
                "yuheng.rules.active", "{}", "a".repeat(64), null, scope.engineRole().name(), null,
                GatewayPublicationStatusEnum.PLANNED, null, null, now, now, scope);
    }

    private void seedRelease(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT INTO gateway_group (id, gateway_group_code, display_name, env, namespace,
                    created_at, created_by, updated_at, updated_by)
                VALUES ('group-1', 'test', 'Test', 'test', 'default', now(), 'test', now(), 'test')
                """);
        jdbc.update("""
                INSERT INTO gateway_release (id, gateway_group_id, draft_revision, status,
                    validation_report, structured_diff, change_reason, created_at, created_by, updated_at)
                VALUES ('release-1', 'group-1', 1, 'PUBLISHING', '{}', '{}', 'test', now(), 'test', now())
                """);
        jdbc.update("""
                INSERT INTO gateway_release_attempt (release_id, attempt_no, status, started_at)
                VALUES ('release-1', 1, 'PUBLISHING', now())
                """);
    }
}
