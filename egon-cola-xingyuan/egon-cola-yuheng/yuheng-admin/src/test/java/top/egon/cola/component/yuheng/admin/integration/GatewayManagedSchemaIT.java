package top.egon.cola.component.yuheng.admin.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlResult;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO;
import top.egon.cola.component.common.mybatis.ddl.EgonColaPostgreDdlRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 中文说明：{@code GatewayManagedSchemaIT} 在真实 Postgres 上验收受管 DDL 的库级这一半——只有
 * MyBatis→guard→ShardingSphere→PG 这条路径真跑起来才算数：受管 runner 以 {@code SHARD} 角色把 {@code db/egon-mp}
 * 清单里的唯一一版 SQL 应用到一张空 schema，49 张逻辑 {@code *_t0} 表与 {@code ddl_history} 就位；SQL 文本里声明的
 * 每一个命名约束/索引都必须被库真实物化（既不少也不多）；软删除业务唯一键必须真的把 {@code deleted_at} 纳入组合键、
 * “存活”唯一键必须真的排除已删行；重复软删除在同一微秒必须真的冲突；pgvector 列必须真的是 {@code vector} 类型。
 * 期望值全部来自 SQL 文本本身而不是库现状的镜像，因此本类不会把“实现后来长什么样”反向固化成断言。
 * English summary: {@code GatewayManagedSchemaIT} accepts the database half of the managed DDL on a real Postgres, which
 * only counts through the MyBatis→guard→ShardingSphere→PG path: the managed runner applies the single SQL version of the
 * {@code db/egon-mp} manifest as role {@code SHARD} into an empty schema, the 49 logical {@code *_t0} tables plus
 * {@code ddl_history} exist, every named constraint and index declared in the SQL text is really materialized (neither
 * missing nor invented), a soft-delete business key really carries {@code deleted_at} while the "active" key really
 * excludes deleted rows, a repeated soft delete in the same microsecond really collides, and the pgvector column really
 * is of type {@code vector}. Every expectation is derived from the SQL text rather than mirrored off the live schema, so
 * this class never freezes whatever the database happens to look like today.
 *
 * 用法 / Usage: 仅在显式提供隔离验收库 {@code YUHENG_MANAGED_TEST_POSTGRES_URL/USER/PASSWORD} 时运行，schema 名随机、
 * 结束即删；本仓库不启动 Docker、不起常驻进程，也没有任何镜像自带 pgvector，所以缺这三项环境变量时本类整体不执行——
 * 那正是 Plan §8 "PG/sharding/vector" 闸门的“缺环境必须标 SKIPPED 未验收”，不能当作 GREEN。
 * 库级不变量之外的读写语义（CAS 谓词、0 行不判成功、租户注入）由 {@code GatewayPublicationMpTest} 等受守卫 MP 边界
 * 测试负责，本类不重复。/ Run only when an isolated acceptance database is supplied explicitly; the schema name is
 * random and dropped afterwards. This repository starts no Docker, no long-running process, and no shipped image carries
 * pgvector, so without those three variables the class does not execute at all — exactly the Plan's "missing environment
 * is reported SKIPPED, never accepted" gate, not GREEN. Read/write semantics beyond the database invariants belong to
 * the guarded MP boundary tests such as {@code GatewayPublicationMpTest} and are not repeated here.
 */
@EnabledIfEnvironmentVariable(
        named = "YUHENG_MANAGED_TEST_POSTGRES_URL",
        matches = ".+"
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GatewayManagedSchemaIT {

    /** 受管 SQL 与清单的 classpath 坐标，与生产 runner 完全一致 / the same classpath coordinates the production runner uses. */
    private static final String SQL_RESOURCE = "db/egon-mp/20260922_001_yuheng_schema.sql";

    private static final String MANIFEST_RESOURCE = "db/egon-mp/repository-manifest.json";

    /** 逻辑表数量，与 Spec 修订 §7 的 49 张表一致 / the 49 logical tables of the revised Spec section 7. */
    private static final int DECLARED_TABLE_COUNT = 49;

    private static final Pattern DECLARED_TABLE = Pattern.compile("CREATE TABLE public\\.([a-z0-9_]+) \\(");

    private static final Pattern DECLARED_CONSTRAINT = Pattern.compile("CONSTRAINT ([a-z0-9_]+)\\s+");

    private static final Pattern DECLARED_INDEX = Pattern.compile("CREATE (?:UNIQUE )?INDEX ([a-z0-9_]+)\\b");

    /** 只比较脚本命名规范内的约束：列内联 CHECK 由 PG 自动命名，不在声明集合里。
     * Only the naming convention the script declares is compared; inline column checks carry generated names. */
    private static final List<String> DECLARED_CONSTRAINT_PREFIXES = List.of("pk_", "uq_", "fk_", "ck_");

    /** SQLSTATE 23505：唯一约束或唯一索引冲突，PostgreSQL 与 JDBC 映射无关 / unique violation, independent of driver mapping. */
    private static final String UNIQUE_VIOLATION = "23505";

    private static final String NO_ERROR = "00000";

    private final String jdbcUrl = requiredEnvironment("YUHENG_MANAGED_TEST_POSTGRES_URL");

    private final String user = requiredEnvironment("YUHENG_MANAGED_TEST_POSTGRES_USER");

    private final String password = requiredEnvironment("YUHENG_MANAGED_TEST_POSTGRES_PASSWORD");

    private final String schema = "yuheng_managed_" + suffix();

    private final String foreignSchema = "yuheng_foreign_" + suffix();

    private final EgonColaDdlManifestBO manifest = readManifest();

    private final String routeFingerprint = manifest.scripts().get(0).sha256();

    /** runner 首次应用的结果，留给断言使用而不是在装配里自证 / the first apply outcome, asserted later not trusted inline. */
    private List<EgonColaDdlResult> firstRun;

    @BeforeAll
    void applyTheManagedBaselineIntoAFreshEmptySchema() throws SQLException {
        execute("CREATE SCHEMA " + schema);
        firstRun = runner().run(List.of(target(schema)));
    }

    @AfterAll
    void dropTheAcceptanceSchemas() throws SQLException {
        execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        execute("DROP SCHEMA IF EXISTS " + foreignSchema + " CASCADE");
    }

    @Test
    @DisplayName("受管 runner 以 SHARD 角色应用清单，49 张逻辑表与 ddl_history 恰好就位")
    void appliesTheDeclaredTablesAndRegistersExactlyOneManagedVersion() throws SQLException {
        assertThat(manifest.family()).isEqualTo("web");
        assertThat(manifest.scripts()).hasSize(1);
        assertThat(firstRun).hasSize(1);
        assertThat(firstRun.get(0).status()).isEqualTo(EgonColaDdlResult.StatusEnum.APPLIED);
        assertThat(firstRun.get(0).version()).isEqualTo(manifest.scripts().get(0).version());
        assertThat(firstRun.get(0).checksum()).isEqualTo(routeFingerprint);

        Set<String> declaredTables = declaredNames(DECLARED_TABLE, sql());
        assertThat(declaredTables)
                .as("the script declares the approved number of logical tables")
                .hasSize(DECLARED_TABLE_COUNT);
        assertThat(relations())
                .as("nothing but the declared tables and the runner journal exists in the schema")
                .containsExactlyInAnyOrderElementsOf(withJournal(declaredTables));

        assertThat(runner().run(List.of(target(schema))).get(0).status())
                .as("a second managed run never re-executes the applied version")
                .isEqualTo(EgonColaDdlResult.StatusEnum.SKIPPED);
        assertThat(scalar("SELECT count(*) FROM " + schema + ".ddl_history")).isEqualTo("1");
        assertThat(scalar("SELECT script || '|' || type || '|' || version || '|' || checksum || '|'"
                + " || route_fingerprint || '|' || tenant_id FROM " + schema + ".ddl_history"))
                .isEqualTo(manifest.scripts().get(0).path() + "|SQL|"
                        + manifest.scripts().get(0).version() + "|" + routeFingerprint + "|"
                        + routeFingerprint + "|0");
    }

    @Test
    @DisplayName("SQL 文本声明的每个命名约束与索引都被库物化，库里也没有未声明的额外约束")
    void materializesExactlyTheDeclaredConstraintsAndIndexes() throws SQLException {
        assertThat(liveConstraints())
                .as("every named constraint of the script exists in the database, and no other does")
                .containsExactlyInAnyOrderElementsOf(declaredNames(DECLARED_CONSTRAINT, sql()));
        assertThat(liveIndexes())
                .as("every standalone index of the script exists, and none is invented")
                .containsExactlyInAnyOrderElementsOf(declaredNames(DECLARED_INDEX, sql()));
    }

    @Test
    @DisplayName("软删除唯一键真的组合 deleted_at，存活唯一键真的排除已删行")
    void combinesDeletedAtIntoLifecycleKeysAndExcludesDeletedRowsFromActiveKeys() throws SQLException {
        List<String> lifecycleDefinitions = query("SELECT pg_get_constraintdef(c.oid) FROM pg_catalog.pg_constraint c "
                + "JOIN pg_catalog.pg_namespace n ON n.oid=c.connamespace "
                + "WHERE n.nspname='" + schema + "' AND c.conname LIKE 'uq!_%!_life!_%' ESCAPE '!'");
        assertThat(lifecycleDefinitions)
                .as("the script must actually declare lifecycle keys before this proves anything")
                .isNotEmpty();
        assertThat(lifecycleDefinitions)
                .as("every lifecycle unique key ends with the soft-delete column, so a repeated delete stays distinct")
                .allSatisfy(definition -> assertThat(definition).matches("(?s).*\\(.*deleted_at\\)$"));

        List<String> activeDefinitions = query("SELECT indexdef FROM pg_indexes WHERE schemaname='" + schema + "'"
                + " AND indexname LIKE 'uq!_%!_active!_%' ESCAPE '!'");
        assertThat(activeDefinitions)
                .as("the script must actually declare active keys")
                .isNotEmpty();
        assertThat(activeDefinitions)
                .as("an active key covers live rows only, which is what makes deleted_at NULL mean \"not deleted\"")
                .allSatisfy(definition -> assertThat(definition).contains("WHERE (deleted_at IS NULL)"));
    }

    @Test
    @DisplayName("同一业务键下 NULL 语义、重复软删除同微秒冲突、异微秒可共存都被库强制")
    void provesNullActiveAndRepeatedMicrosecondSoftDeleteSemantics() throws SQLException {
        String live = "nonce-live-" + suffix();
        String retired = "nonce-retired-" + suffix();
        String sameMicrosecond = "2026-09-22 08:30:00.123456";
        String laterMicrosecond = "2026-09-22 08:30:00.123457";

        assertThat(sqlStateOf(() -> insertNonce(7_001L, live, null)))
                .as("the first live row is accepted")
                .isEqualTo(NO_ERROR);
        assertThat(sqlStateOf(() -> insertNonce(7_002L, live, null)))
                .as("two live rows with one business key must not coexist even though deleted_at is NULL for both")
                .isEqualTo(UNIQUE_VIOLATION);

        assertThat(sqlStateOf(() -> insertNonce(7_003L, retired, sameMicrosecond)))
                .as("a soft-deleted intent is a lifecycle row, not a blocked key")
                .isEqualTo(NO_ERROR);
        assertThat(sqlStateOf(() -> insertNonce(7_004L, retired, laterMicrosecond)))
                .as("the same business key deleted at a later microsecond is a distinct lifecycle row")
                .isEqualTo(NO_ERROR);
        assertThat(sqlStateOf(() -> insertNonce(7_005L, retired, sameMicrosecond)))
                .as("re-deleting in the same microsecond is exactly the collision the caller answers with a "
                        + "fresh-transaction retry")
                .isEqualTo(UNIQUE_VIOLATION);
        assertThat(scalar("SELECT count(*) FROM " + schema + ".gateway_hmac_nonce_t0 WHERE nonce = '" + retired + "'"))
                .as("only the distinct-microsecond pair survived")
                .isEqualTo("2");
    }

    @Test
    @DisplayName("pgvector 列以 vector 类型物化并带维度 CHECK，缺扩展时脚本本身就会失败")
    void materializesTheVectorColumnWithItsDimensionCheck() throws SQLException {
        assertThat(scalar("SELECT udt_name FROM information_schema.columns "
                + "WHERE table_schema='" + schema + "' AND table_name='gateway_knowledge_chunk_t0' "
                + "AND column_name='embedding'"))
                .as("an untyped column would silently accept anything")
                .isEqualTo("vector");
        assertThat(query("SELECT pg_get_constraintdef(c.oid) FROM pg_catalog.pg_constraint c "
                + "JOIN pg_catalog.pg_namespace n ON n.oid=c.connamespace "
                + "WHERE n.nspname='" + schema + "' AND c.contype='c' AND c.conrelid='"
                + schema + ".gateway_knowledge_chunk_t0'::regclass"))
                .as("the stored dimensionality is enforced by the database, not by the application")
                .anySatisfy(definition -> assertThat(definition).contains("vector_dims"));
    }

    @Test
    @DisplayName("runner 拒绝非空的未知 schema，绝不静默补建或本机修复")
    void refusesAnUnknownNonEmptySchemaInsteadOfRepairingIt() throws SQLException {
        execute("CREATE SCHEMA " + foreignSchema);
        execute("CREATE TABLE " + foreignSchema + ".unmanaged_legacy (id bigint PRIMARY KEY)");
        assertThatThrownBy(() -> runner().run(List.of(target(foreignSchema))))
                .as("an occupied, unjournalable schema is a deployment error, never something to repair in place")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REBUILD_REQUIRED");
    }

    private EgonColaPostgreDdlRunner runner() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        return new EgonColaPostgreDdlRunner(
                new ValidationUtils(validator),
                new PathMatchingResourcePatternResolver(),
                Clock.systemUTC(),
                Duration.ofSeconds(10),
                Duration.ofSeconds(60));
    }

    private EgonColaDdlTargetBO target(String targetSchema) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        return new EgonColaDdlTargetBO("yuheng_0", targetSchema, EgonColaDdlTargetBO.RoleEnum.SHARD,
                (DataSource) dataSource, manifest, routeFingerprint);
    }

    private static EgonColaDdlManifestBO readManifest() {
        return (EgonColaDdlManifestBO) readResource(MANIFEST_RESOURCE, stream -> {
            try {
                return new ObjectMapper().readValue(stream, EgonColaDdlManifestBO.class);
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        });
    }

    private static String sql() {
        return (String) readResource(SQL_RESOURCE, stream -> {
            try {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        });
    }

    private static Object readResource(String resource, java.util.function.Function<InputStream, Object> reader) {
        try (InputStream stream = GatewayManagedSchemaIT.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException(resource + " is missing from the classpath");
            }
            return reader.apply(stream);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static Set<String> declaredNames(Pattern pattern, String text) {
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private static Set<String> withJournal(Set<String> tables) {
        Set<String> expected = new LinkedHashSet<>(tables);
        expected.add("ddl_history");
        return expected;
    }

    private void insertNonce(long id, String nonce, String deletedAt) throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO " + schema + ".gateway_hmac_nonce_t0 (id, tenant_id, create_user_id, create_time,"
                             + " update_user_id, update_time, deleted_at, version, access_key, nonce, expires_at)"
                             + " VALUES (?,9001,'it',CURRENT_TIMESTAMP,'it',CURRENT_TIMESTAMP,?::timestamp(6),0,"
                             + "'ak-contract',?,CURRENT_TIMESTAMP + interval '1 hour')")) {
            statement.setLong(1, id);
            statement.setString(2, deletedAt);
            statement.setString(3, nonce);
            statement.executeUpdate();
        }
    }

    private static String sqlStateOf(SqlAction action) throws SQLException {
        try {
            action.run();
            return NO_ERROR;
        } catch (SQLException failure) {
            return failure.getSQLState();
        }
    }

    /** 一次可失败的写库动作 / one fallible database write. */
    private interface SqlAction {
        void run() throws SQLException;
    }

    private Set<String> relations() throws SQLException {
        return new LinkedHashSet<>(query("SELECT c.relname FROM pg_catalog.pg_class c "
                + "JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace "
                + "WHERE n.nspname='" + schema + "' AND c.relkind IN ('r','p','v','m','S','f')"));
    }

    private Set<String> liveConstraints() throws SQLException {
        Set<String> names = new LinkedHashSet<>();
        for (String name : query("SELECT c.conname FROM pg_catalog.pg_constraint c "
                + "JOIN pg_catalog.pg_namespace n ON n.oid=c.connamespace WHERE n.nspname='" + schema + "'")) {
            if (DECLARED_CONSTRAINT_PREFIXES.stream().anyMatch(name::startsWith)) {
                names.add(name);
            }
        }
        return names;
    }

    /** 独立索引 = 全部索引减去与命名约束同名的支撑索引。
     * Standalone indexes are every index minus the ones backing a named constraint. */
    private Set<String> liveIndexes() throws SQLException {
        List<String> indexes = new ArrayList<>(
                query("SELECT indexname FROM pg_indexes WHERE schemaname='" + schema + "'"));
        indexes.removeAll(liveConstraints());
        return new LinkedHashSet<>(indexes);
    }

    private String scalar(String sql) throws SQLException {
        List<String> values = query(sql);
        return values.isEmpty() ? null : String.valueOf(values.get(0));
    }

    private List<String> query(String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                values.add(String.valueOf(rows.getObject(1)));
            }
        }
        return values;
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be provided for the managed-schema acceptance run");
        }
        return value;
    }
}
