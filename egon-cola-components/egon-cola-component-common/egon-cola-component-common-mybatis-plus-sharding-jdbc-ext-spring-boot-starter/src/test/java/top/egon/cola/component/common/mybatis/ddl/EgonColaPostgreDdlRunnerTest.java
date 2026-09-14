package top.egon.cola.component.common.mybatis.ddl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO.ScriptBO;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static top.egon.cola.component.common.mybatis.ddl.EgonColaDdlInitializationTest.*;

class EgonColaPostgreDdlRunnerTest {

    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidators() {
        VALIDATORS.close();
    }

    static EgonColaPostgreDdlRunner runner() {
        return new EgonColaPostgreDdlRunner(new ValidationUtils(VALIDATORS.getValidator()),
                new PathMatchingResourcePatternResolver(), Clock.systemUTC(), Duration.ofSeconds(10), Duration.ofSeconds(60));
    }


    @Test
    void manifestUsesJacksonRecordsAndImmutableOrderedVersions() throws Exception {
        var manifest = manifest(2);
        ObjectMapper mapper = new ObjectMapper();
        assertThat(mapper.readValue(mapper.writeValueAsString(manifest), EgonColaDdlManifestBO.class)).isEqualTo(manifest);
        assertThatThrownBy(() -> manifest.scripts().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new EgonColaDdlManifestBO("light", List.of(manifest.scripts().getLast(), manifest.scripts().getFirst())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ddlTargetAdaptsIddlWithoutExposingDataSourceInLogsOrJson() throws Exception {
        DataSource source = mock(DataSource.class);
        when(source.toString()).thenReturn("PASSWORD_MUST_NOT_APPEAR");
        var target = target("primary", source, manifest(1));
        assertThat(target.getSqlFiles()).containsExactly("ddl/initial-test.sql");
        assertThat(target.getDdlGenerator().getDdlHistory()).isEqualTo("\"public\".\"ddl_history\"");
        var seen = new java.util.concurrent.atomic.AtomicReference<DataSource>();
        target.runScript(seen::set);
        assertThat(seen.get()).isSameAs(source);
        assertThat(target.toString()).doesNotContain("PASSWORD_MUST_NOT_APPEAR");
        assertThat(new ObjectMapper().writeValueAsString(target)).doesNotContain("dataSource", "ddlGenerator");
    }

    @Test
    void rejectsShadowedClasspathSqlBeforeConnections() throws Exception {
        ResourcePatternResolver resources = mock(ResourcePatternResolver.class);
        Resource resource = new ByteArrayResource("CREATE TABLE test(id bigint);".getBytes(StandardCharsets.UTF_8));
        when(resources.getResources("classpath*:ddl/initial-test.sql")).thenReturn(new Resource[]{resource, resource});
        DataSource source = mock(DataSource.class);
        var runner = new EgonColaPostgreDdlRunner(new ValidationUtils(VALIDATORS.getValidator()), resources,
                Clock.systemUTC(), Duration.ofSeconds(10), Duration.ofSeconds(60));
        assertThatThrownBy(() -> runner.run(List.of(target("primary", source, manifest(1)))))
                .hasMessageContaining("AMBIGUOUS_SQL_RESOURCE");
        verifyNoInteractions(source);
    }

    @Test
    void rejectsTransactionControlAndNontransactionalSqlBeforeConnections() throws Exception {
        for (String sql : List.of("COMMIT;", "ROLLBACK;", "BEGIN; CREATE TABLE test(id bigint);",
                "CREATE INDEX CONCURRENTLY idx ON test(id);", "VACUUM test;", "ALTER SYSTEM SET work_mem='8MB';",
                "CREATE DATABASE other;", "END;", "START TRANSACTION;", "PREPARE TRANSACTION 'x';")) {
            ResourcePatternResolver resources = mock(ResourcePatternResolver.class);
            byte[] content = sql.getBytes(StandardCharsets.UTF_8);
            when(resources.getResources("classpath*:ddl/initial-test.sql")).thenReturn(new Resource[]{new ByteArrayResource(content)});
            var manifest = new EgonColaDdlManifestBO("light", List.of(new ScriptBO("20260913_001", "ddl/initial-test.sql",
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)))));
            DataSource source = mock(DataSource.class);
            var runner = new EgonColaPostgreDdlRunner(new ValidationUtils(VALIDATORS.getValidator()), resources,
                    Clock.systemUTC(), Duration.ofSeconds(10), Duration.ofSeconds(60));
            assertThatThrownBy(() -> runner.run(List.of(target("primary", source, manifest))))
                    .as(sql).hasMessageContaining("NONTRANSACTIONAL_SQL");
            verifyNoInteractions(source);
        }
    }

    @Test
    void splitsPostgreSqlDollarBodiesWithoutBreakingTheirInternalSemicolons() {
        String sql = "-- setup\nDO $egon$\nBEGIN\n    IF true THEN\n        RAISE NOTICE 'inner; semicolon';\n    END IF;\nEND\n$egon$;\nCREATE TABLE sample (value text DEFAULT 'a;b');\n/* a; comment */ SELECT 1;";
        assertThat(EgonColaPostgreDdlRunner.splitPostgreSqlStatements(sql)).containsExactly(
                "-- setup\nDO $egon$\nBEGIN\n    IF true THEN\n        RAISE NOTICE 'inner; semicolon';\n    END IF;\nEND\n$egon$",
                "CREATE TABLE sample (value text DEFAULT 'a;b')",
                "/* a; comment */ SELECT 1");
        assertThat(EgonColaPostgreDdlRunner.splitPostgreSqlStatements("SELECT 1; -- footer\n/* only comment; */"))
                .containsExactly("SELECT 1");
        assertThat(EgonColaPostgreDdlRunner.splitPostgreSqlStatements("SELECT \"a;b\"; SELECT 2"))
                .containsExactly("SELECT \"a;b\"", "SELECT 2");
    }

    @Test
    void rejectsRepeatedPhysicalTargetBeforeAnyDdl() throws Exception {
        var jdbc = fixture(List.of(), false);
        var target = target("primary", jdbc.source(), manifest(1));
        assertThatThrownBy(() -> runner().run(List.of(target, target))).hasMessageContaining("DUPLICATE_DDL_TARGET");
        verifyNoInteractions(jdbc.sql());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "EGON_MP_PG_DDL_TEST", matches = "true")
    void explicitlyEnabledPostgreSqlSchemaInitializesUpgradesAndSkips() throws Exception {
        PGSimpleDataSource admin = new PGSimpleDataSource();
        admin.setURL(System.getenv("EGON_MP_PG_URL"));
        admin.setUser(System.getenv("EGON_MP_PG_USER"));
        admin.setPassword(System.getenv("EGON_MP_PG_PASSWORD"));
        String schema = "egon_mp_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = admin.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        try {
            PGSimpleDataSource scoped = new PGSimpleDataSource();
            scoped.setURL(System.getenv("EGON_MP_PG_URL"));
            scoped.setUser(System.getenv("EGON_MP_PG_USER"));
            scoped.setPassword(System.getenv("EGON_MP_PG_PASSWORD"));
            scoped.setCurrentSchema(schema);
            var initial = new EgonColaDdlTargetBO("primary", schema, EgonColaDdlTargetBO.RoleEnum.SHARD, scoped, manifest(1), ROUTE);
            assertThat(runner().run(List.of(initial))).extracting(EgonColaDdlResult::status)
                    .containsExactly(EgonColaDdlResult.StatusEnum.APPLIED);
            var next = new EgonColaDdlTargetBO("primary", schema, EgonColaDdlTargetBO.RoleEnum.SHARD, scoped, manifest(2), ROUTE);
            DataSource failingSource = mock(DataSource.class);
            when(failingSource.getConnection()).thenAnswer(ignored -> {
                var physical = scoped.getConnection();
                var failing = mock(java.sql.Connection.class, org.mockito.AdditionalAnswers.delegatesTo(physical));
                doAnswer(call -> {
                    var history = spy(physical.prepareStatement((String) call.getArgument(0)));
                    doThrow(new java.sql.SQLException("injected history failure", "23514")).when(history).executeUpdate();
                    return history;
                }).when(failing).prepareStatement(org.mockito.ArgumentMatchers.startsWith("INSERT INTO"));
                return failing;
            });
            var failed = new EgonColaDdlTargetBO("primary", schema, EgonColaDdlTargetBO.RoleEnum.SHARD, failingSource, manifest(2), ROUTE);
            assertThatThrownBy(() -> runner().run(List.of(failed))).hasMessageContaining("23514");
            try (var connection = scoped.getConnection(); var statement = connection.prepareStatement(
                    "SELECT count(*) FROM information_schema.columns WHERE table_schema=? AND table_name='runner_test' AND column_name='description'")) {
                statement.setString(1, schema);
                try (var rows = statement.executeQuery()) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getInt(1)).isZero();
                }
            }
            assertThat(runner().run(List.of(next))).extracting(EgonColaDdlResult::status)
                    .containsExactly(EgonColaDdlResult.StatusEnum.SKIPPED, EgonColaDdlResult.StatusEnum.APPLIED);
            assertThat(runner().run(List.of(next))).allSatisfy(result -> assertThat(result.status()).isEqualTo(EgonColaDdlResult.StatusEnum.SKIPPED));
            try (var connection = scoped.getConnection(); var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT count(*) FROM ddl_history WHERE tenant_id=0 AND type='SQL'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isEqualTo(2);
            }
        } finally {
            // Only the UUID schema created by this explicitly opted-in test is disposable.
            try (var connection = admin.getConnection(); var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }
}
