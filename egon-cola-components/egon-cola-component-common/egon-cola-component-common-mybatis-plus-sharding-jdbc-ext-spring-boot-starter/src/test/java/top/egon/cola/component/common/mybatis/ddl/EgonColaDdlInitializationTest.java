package top.egon.cola.component.common.mybatis.ddl;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO.ScriptBO;

import javax.sql.DataSource;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class EgonColaDdlInitializationTest {

    static final String ROUTE = "a".repeat(64);
    static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidators() {
        VALIDATORS.close();
    }

    static EgonColaPostgreDdlRunner runner() {
        return new EgonColaPostgreDdlRunner(new ValidationUtils(VALIDATORS.getValidator()),
                new PathMatchingResourcePatternResolver(), Clock.systemUTC(), Duration.ofSeconds(10), Duration.ofSeconds(60));
    }

    @Test
    void initializesAnEmptySchemaAndSkipsIdenticalManagedVersions() throws Exception {
        JdbcFixtureBO jdbc = fixture(List.of(), false);
        EgonColaDdlTargetBO target = target("primary", jdbc.source(), manifest(1));
        assertThat(runner().run(List.of(target))).singleElement()
                .satisfies(result -> assertThat(result.status()).isEqualTo(EgonColaDdlResult.StatusEnum.APPLIED));
        var order = inOrder(jdbc.connection(), jdbc.sql(), jdbc.historyInsert());
        order.verify(jdbc.connection()).setAutoCommit(false);
        order.verify(jdbc.sql()).execute(contains("CREATE TABLE runner_test"));
        order.verify(jdbc.historyInsert()).executeUpdate();
        order.verify(jdbc.connection()).commit();
        verify(jdbc.connection(), never()).rollback();
        assertThat(jdbc.committed().get()).hasSize(1);
        assertThat(runner().run(List.of(target))).singleElement()
                .satisfies(result -> assertThat(result.status()).isEqualTo(EgonColaDdlResult.StatusEnum.SKIPPED));
        verify(jdbc.sql(), times(2)).execute(anyString());
        verify(jdbc.historyInsert(), times(1)).executeUpdate();
    }

    @Test
    void refusesUnmanagedNonemptySchemaWithoutBusinessDdlOrHistoryAdoption() throws Exception {
        JdbcFixtureBO jdbc = fixture(List.of(), true);
        assertThatThrownBy(() -> runner().run(List.of(target("primary", jdbc.source(), manifest(1)))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("REBUILD_REQUIRED");
        verify(jdbc.sql(), never()).execute(anyString());
        verify(jdbc.historyInsert(), never()).executeUpdate();
        verify(jdbc.connection()).rollback();
        verify(jdbc.connection(), never()).prepareStatement(contains("flyway_schema_history"));
    }

    @Test
    void advancesOnlyTheManifestSuffixAndRollsBackHistoryFailure() throws Exception {
        JdbcFixtureBO jdbc = fixture(List.of(manifest(1).scripts().getFirst()), false);
        doThrow(new SQLException("history write rejected", "23514")).when(jdbc.historyInsert()).executeUpdate();
        assertThatThrownBy(() -> runner().run(List.of(target("primary", jdbc.source(), manifest(2)))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("23514");
        verify(jdbc.sql()).execute(contains("ALTER TABLE runner_test"));
        verify(jdbc.sql(), never()).execute(contains("CREATE TABLE runner_test"));
        verify(jdbc.connection()).rollback();
        verify(jdbc.connection(), never()).commit();
        assertThat(jdbc.committed().get()).hasSize(1);
    }

    @Test
    void rollsBackSqlFailureBeforeWritingHistory() throws Exception {
        JdbcFixtureBO jdbc = fixture(List.of(), false);
        doThrow(new SQLException("ddl failed", "42601")).when(jdbc.sql()).execute(anyString());
        assertThatThrownBy(() -> runner().run(List.of(target("primary", jdbc.source(), manifest(1)))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("42601");
        verify(jdbc.connection()).rollback();
        verify(jdbc.historyInsert(), never()).executeUpdate();
        assertThat(jdbc.committed().get()).isEmpty();
    }

    @Test
    void leavesEarlierTargetsCommittedWhenALaterTargetFails() throws Exception {
        JdbcFixtureBO first = fixture(List.of(), false);
        JdbcFixtureBO second = fixture(List.of(), false);
        doThrow(new SQLException("second target failed", "XX000")).when(second.sql()).execute(anyString());
        var targets = List.of(target("a_primary", first.source(), manifest(1)), target("b_primary", second.source(), manifest(1)));
        assertThatThrownBy(() -> runner().run(targets)).hasMessageContaining("b_primary");
        assertThat(first.committed().get()).hasSize(1);
        assertThat(second.committed().get()).isEmpty();
        verify(first.connection(), never()).rollback();
        verify(second.connection()).rollback();
        reset(second.sql());
        when(second.sql().getUpdateCount()).thenReturn(-1);
        assertThat(runner().run(targets)).extracting(EgonColaDdlResult::status)
                .containsExactly(EgonColaDdlResult.StatusEnum.SKIPPED, EgonColaDdlResult.StatusEnum.APPLIED);
    }

    @Test
    void rejectsChangedChecksumRouteAndManifestPrefix() throws Exception {
        ScriptBO initial = manifest(1).scripts().getFirst();
        for (ScriptBO old : List.of(new ScriptBO(initial.version(), initial.path(), "b".repeat(64)),
                new ScriptBO("20260912_001", initial.path(), initial.sha256()))) {
            JdbcFixtureBO jdbc = fixture(List.of(old), false);
            assertThatThrownBy(() -> runner().run(List.of(target("primary", jdbc.source(), manifest(1)))))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("MISMATCH");
            verify(jdbc.sql(), never()).execute(anyString());
        }
        JdbcFixtureBO jdbc = fixture(List.of(initial), false);
        EgonColaDdlTargetBO altered = new EgonColaDdlTargetBO("primary", "public", EgonColaDdlTargetBO.RoleEnum.SHARD,
                jdbc.source(), manifest(1), "b".repeat(64));
        assertThatThrownBy(() -> runner().run(List.of(altered))).hasMessageContaining("ROUTE_FINGERPRINT_MISMATCH");
        verify(jdbc.sql(), never()).execute(anyString());
    }

    @Test
    void validatesAllResourcesBeforeOpeningAnyConnection() throws Exception {
        JdbcFixtureBO jdbc = fixture(List.of(), false);
        ScriptBO initial = manifest(1).scripts().getFirst();
        EgonColaDdlManifestBO wrong = new EgonColaDdlManifestBO("light", List.of(new ScriptBO(initial.version(), initial.path(), "b".repeat(64))));
        assertThatThrownBy(() -> runner().run(List.of(target("first", jdbc.source(), manifest(1)), target("later", jdbc.source(), wrong))))
                .hasMessageContaining("CHECKSUM_MISMATCH");
        verify(jdbc.source(), never()).getConnection();
        assertThatThrownBy(() -> new EgonColaDdlManifestBO("light", List.of(initial, initial))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScriptBO(initial.version(), "https://example.invalid/init.sql", initial.sha256()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScriptBO(initial.version(), "db/../secrets.sql", initial.sha256()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesPrimaryProductAndSchemaBeforeDdl() throws Exception {
        JdbcFixtureBO mysql = fixture(List.of(), false);
        when(mysql.connection().getMetaData().getDatabaseProductName()).thenReturn("MySQL");
        assertThatThrownBy(() -> runner().run(List.of(target("primary", mysql.source(), manifest(1)))))
                .hasMessageContaining("POSTGRESQL_REQUIRED");
        verify(mysql.sql(), never()).execute(anyString());
        JdbcFixtureBO replica = fixture(List.of(), false);
        when(replica.identity().getBoolean(2)).thenReturn(true);
        assertThatThrownBy(() -> runner().run(List.of(target("primary", replica.source(), manifest(1)))))
                .hasMessageContaining("PRIMARY_REQUIRED");
        verify(replica.sql(), never()).execute(anyString());
        JdbcFixtureBO wrongSchema = fixture(List.of(), false);
        when(wrongSchema.identity().getString(1)).thenReturn("other");
        assertThatThrownBy(() -> runner().run(List.of(target("primary", wrongSchema.source(), manifest(1)))))
                .hasMessageContaining("SCHEMA_MISMATCH");
        verify(wrongSchema.sql(), never()).execute(anyString());
    }

    @Test
    void confirmsAnUnknownCommitUsingManagedHistoryWithoutReexecutingDdl() throws Exception {
        JdbcFixtureBO jdbc = fixture(List.of(), false);
        doAnswer(call -> {
            jdbc.committed().set(List.copyOf(jdbc.pending()));
            throw new SQLException("connection lost after commit", "08006");
        }).when(jdbc.connection()).commit();
        assertThat(runner().run(List.of(target("primary", jdbc.source(), manifest(1)))))
                .singleElement().satisfies(result -> assertThat(result.status()).isEqualTo(EgonColaDdlResult.StatusEnum.APPLIED));
        verify(jdbc.sql(), times(2)).execute(anyString());
        verify(jdbc.source(), times(2)).getConnection();
    }

    @Test
    void doesNotReportSuccessWhenUnknownCommitHasNoMatchingHistory() throws Exception {
        JdbcFixtureBO jdbc = fixture(List.of(), false);
        doThrow(new SQLException("connection lost before commit", "08006")).when(jdbc.connection()).commit();
        assertThatThrownBy(() -> runner().run(List.of(target("primary", jdbc.source(), manifest(1)))))
                .hasMessageContaining("COMMIT_UNKNOWN");
        assertThat(jdbc.committed().get()).isEmpty();
        verify(jdbc.sql(), times(2)).execute(anyString());
    }

    @Test
    void schemaLockFailureRollsBackBeforeInspectingOrChangingBusinessObjects() throws Exception {
        JdbcFixtureBO jdbc = fixture(List.of(), false);
        PreparedStatement lock = mock(PreparedStatement.class);
        when(jdbc.connection().prepareStatement("SELECT pg_advisory_xact_lock(?)")).thenReturn(lock);
        doThrow(new SQLException("lock timeout", "55P03")).when(lock).execute();
        assertThatThrownBy(() -> runner().run(List.of(target("primary", jdbc.source(), manifest(1)))))
                .hasMessageContaining("55P03");
        verify(jdbc.sql(), never()).execute(anyString());
        verify(jdbc.connection(), never()).prepareStatement(contains("pg_catalog.pg_class"));
        verify(jdbc.connection()).rollback();
    }

    @Test
    void acquiresSchemaLockBeforeReadingHistoryAndSetsTrustedRoleWithinTheTransaction() throws Exception {
        JdbcFixtureBO jdbc = fixture(List.of(), false);
        runner().run(List.of(target("primary", jdbc.source(), manifest(1))));
        var calls = mockingDetails(jdbc.connection()).getInvocations().stream()
                .filter(call -> call.getMethod().getName().equals("prepareStatement"))
                .map(call -> (String) call.getArgument(0)).toList();
        int lockIndex = calls.indexOf("SELECT pg_advisory_xact_lock(?)");
        int stateIndex = java.util.stream.IntStream.range(0, calls.size())
                .filter(index -> calls.get(index).contains("pg_catalog.pg_class")).findFirst().orElseThrow();
        assertThat(lockIndex).isGreaterThanOrEqualTo(0).isLessThan(stateIndex);
        assertThat(calls).anySatisfy(sql -> assertThat(sql).contains("set_config('egon_migration.role', ?, true)"));
    }

    static ScriptBO script(String version, String resource) throws Exception {
        try (var input = new ClassPathResource(resource).getInputStream()) {
            return new ScriptBO(version, resource, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes())));
        }
    }

    static EgonColaDdlManifestBO manifest(int count) throws Exception {
        var scripts = new ArrayList<ScriptBO>();
        scripts.add(script("20260913_001", "ddl/initial-test.sql"));
        if (count == 2) {
            scripts.add(script("20260913_002", "ddl/second-test.sql"));
        }
        return new EgonColaDdlManifestBO("light", scripts);
    }

    static EgonColaDdlTargetBO target(String alias, DataSource source, EgonColaDdlManifestBO manifest) {
        return new EgonColaDdlTargetBO(alias, "public", EgonColaDdlTargetBO.RoleEnum.SHARD, source, manifest, ROUTE);
    }

    static JdbcFixtureBO fixture(List<ScriptBO> installed, boolean unmanaged) throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement sql = mock(Statement.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        PreparedStatement insert = mock(PreparedStatement.class);
        ResultSet identity = mock(ResultSet.class);
        AtomicReference<List<ScriptBO>> committed = new AtomicReference<>(List.copyOf(installed));
        List<ScriptBO> pending = new ArrayList<>(installed);
        Map<Integer, String> parameters = new HashMap<>();
        when(source.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.createStatement()).thenReturn(sql);
        when(sql.getUpdateCount()).thenReturn(-1);
        when(identity.next()).thenReturn(true);
        when(identity.getString(1)).thenReturn("public");
        when(connection.prepareStatement(anyString())).thenAnswer(call -> {
            String text = call.getArgument(0);
            if (text.startsWith("INSERT INTO")) {
                return insert;
            }
            PreparedStatement statement = mock(PreparedStatement.class);
            if (text.contains("pg_is_in_recovery")) {
                when(statement.executeQuery()).thenReturn(identity);
            } else if (text.contains("pg_catalog.pg_class")) {
                when(statement.executeQuery()).thenAnswer(ignored -> {
                    List<String> relations = committed.get().isEmpty() ? (unmanaged ? List.of("old_table") : List.of())
                            : List.of("ddl_history", "runner_test");
                    AtomicInteger index = new AtomicInteger(-1);
                    ResultSet rows = mock(ResultSet.class);
                    when(rows.next()).thenAnswer(next -> index.incrementAndGet() < relations.size());
                    when(rows.getString(1)).thenAnswer(get -> relations.get(index.get()));
                    return rows;
                });
            } else if (text.contains("FROM \"public\".\"ddl_history\"")) {
                when(statement.executeQuery()).thenAnswer(ignored -> {
                    List<ScriptBO> snapshot = committed.get();
                    AtomicInteger index = new AtomicInteger(-1);
                    ResultSet rows = mock(ResultSet.class);
                    when(rows.next()).thenAnswer(next -> index.incrementAndGet() < snapshot.size());
                    when(rows.getString(anyInt())).thenAnswer(get -> {
                        ScriptBO script = snapshot.get(index.get());
                        return switch ((int) get.getArgument(0)) {
                            case 1 -> script.path();
                            case 2 -> "SQL";
                            case 3 -> script.version();
                            case 4 -> script.sha256();
                            case 5 -> ROUTE;
                            default -> throw new IllegalArgumentException();
                        };
                    });
                    return rows;
                });
            }
            return statement;
        });
        doAnswer(call -> { parameters.put(call.getArgument(0), call.getArgument(1)); return null; })
                .when(insert).setString(anyInt(), anyString());
        when(insert.executeUpdate()).thenAnswer(call -> {
            pending.add(new ScriptBO(parameters.get(3), parameters.get(1), parameters.get(4)));
            return 1;
        });
        doAnswer(call -> { committed.set(List.copyOf(pending)); return null; }).when(connection).commit();
        doAnswer(call -> { pending.clear(); pending.addAll(committed.get()); return null; }).when(connection).rollback();
        return new JdbcFixtureBO(source, connection, sql, insert, identity, committed, pending);
    }

    record JdbcFixtureBO(DataSource source, Connection connection, Statement sql, PreparedStatement historyInsert,
                         ResultSet identity, AtomicReference<List<ScriptBO>> committed, List<ScriptBO> pending) {}
}
