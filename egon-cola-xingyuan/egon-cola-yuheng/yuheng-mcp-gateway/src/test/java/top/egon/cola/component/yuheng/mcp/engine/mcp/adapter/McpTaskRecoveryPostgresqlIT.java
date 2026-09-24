package top.egon.cola.component.yuheng.mcp.engine.mcp.adapter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 中文说明：{@code McpTaskRecoveryPostgresqlIT} 验收 MCP 任务恢复的“共享库”那一半：控制面与数据面确实指向同一张
 * {@code gateway_mcp_task_instance}（同一份受管 SQL 的同一指纹、逐列一致的投影），而认领互斥不是 Mock 的自我实现——
 * 占有者、租约到期、业务 {@code revision}、技术 {@code version}、租户范围与 {@code task_key} 幂等键在真 Postgres 上
 * 各自命中或各自落空，影响 0 行的分支绝不会被读成成功。协议任务标识必须是不可翻译的 {@code varchar} 字符串
 * （绝不 {@code parseUUID}、绝不 base64 转 {@code Long}），这条按列类型与投影断言。
 * English summary: {@code McpTaskRecoveryPostgresqlIT} accepts the shared-database half of MCP task recovery: both planes
 * really address one {@code gateway_mcp_task_instance} (one managed SQL script under one fingerprint, column-for-column
 * identical projections), and claim mutual exclusion is not a mock implementing itself - owner, lease expiry, business
 * {@code revision}, technical {@code version}, tenant scoping and the {@code task_key} idempotency key each hit or miss on
 * a real Postgres, so a zero-row branch is never read back as success. Because the protocol task identifier must stay an
 * untranslatable {@code varchar} (no {@code parseUUID}, no base64-to-{@code Long}), that is asserted through the column
 * type and the projections.
 *
 * 用法 / Usage: 不依赖 Spring 上下文、不启动 Docker、不起常驻进程。静态部分总是执行；需要真实库的部分在内层类，只有显式
 * 提供 {@code YUHENG_MANAGED_TEST_POSTGRES_URL/USER/PASSWORD} 时才运行，schema 名随机、结束即删。缺环境时内层类整体
 * SKIPPED，即 Plan §8 的“缺环境必须标 SKIPPED 未验收”，不能当作 GREEN。受守卫 MP 路径本身（租户注入、乐观锁插件、
 * {@code EgonColaRepository} 写守卫）由 {@code McpGatewayEngineTaskStoreConfigurationTest} 等配置边界测试负责，本类只补
 * 它们在库里的真语义反例。/ The static part always executes; the database part lives in the nested class and runs only when
 * an isolated acceptance database is supplied explicitly, under a random schema that is dropped afterwards. Without those
 * variables it is SKIPPED, which is the Plan's "missing environment is reported SKIPPED, never accepted", not GREEN.
 * The guarded MyBatis-Plus path itself belongs to the configuration-boundary tests and is not repeated here.
 */
class McpTaskRecoveryPostgresqlIT {

    /** 受管 SQL 与清单：与 {@code yuheng-admin} 完全同一份产物，数据面不复制第二份 schema。 */
    private static final Path MANAGED_SCRIPT =
            Path.of("../yuheng-admin/src/main/resources/db/egon-mp/20260922_001_yuheng_schema.sql");

    private static final Path MANIFEST =
            Path.of("../yuheng-admin/src/main/resources/db/egon-mp/repository-manifest.json");

    private static final Path DATA_PLANE_MAPPER =
            Path.of("src/main/resources/mybatis/mapper/mcp/McpTaskDAO.xml");

    private static final Path CONTROL_PLANE_MAPPER =
            Path.of("../yuheng-admin/src/main/resources/mybatis/mapper/mcp/McpTaskDAO.xml");

    private static final Path ROW_MODEL =
            Path.of("src/main/java/top/egon/cola/component/yuheng/mcp/engine/mcp/domain/po/McpTaskRecordPO.java");

    /** 逻辑表名由 ShardingSphere 改写为 {@code *_t0}，两侧 XML 与行模型只允许出现逻辑名。 */
    private static final String LOGICAL_TABLE = "gateway_mcp_task_instance";

    private static final String PHYSICAL_TABLE = LOGICAL_TABLE + "_t0";

    private static final Pattern SHA256_ENTRY = Pattern.compile("\"sha256\"\\s*:\\s*\"([0-9a-f]{64})\"");

    private static final Pattern MAPPED_STATEMENT = Pattern.compile(
            "<(select|update|insert|delete)\\b[^>]*\\bid=\"([^\"]+)\"[^>]*>(.*?)</\\1>",
            Pattern.DOTALL);

    private static final Pattern INDEX_STATEMENT = Pattern.compile(
            "CREATE INDEX idx_gateway_mcp_task_\\w+\\s+ON\\s+public\\." + PHYSICAL_TABLE + "[^;]*;",
            Pattern.DOTALL);

    /** SQLSTATE 23505 唯一冲突、23514 CHECK 冲突，与驱动映射无关。 */
    private static final String UNIQUE_VIOLATION = "23505";

    private static final String CHECK_VIOLATION = "23514";

    private static final long TENANT = 9001L;

    private static final long ADJOINING_TENANT = 9002L;

    private static final int SHARED_INDEX_COUNT = 3;

    @Test
    @DisplayName("两个进程投影同一张共享 MCP 表，列集合逐列一致")
    void bothPlanesProjectExactlyTheColumnsTheManagedTableDeclares() {
        List<String> declared = declaredColumns();

        assertThat(mapperColumns(DATA_PLANE_MAPPER)).as("data plane projection").containsExactlyElementsOf(declared);
        assertThat(mapperColumns(CONTROL_PLANE_MAPPER)).as("control plane projection")
                .containsExactlyElementsOf(declared);
        assertThat(declared).contains(
                "id",
                "tenant_id",
                "create_time",
                "update_time",
                "deleted_at",
                "version",
                "worker_owner",
                "lease_until",
                "revision",
                "task_key");
    }

    @Test
    @DisplayName("数据面用的就是控制面那份受管 SQL：指纹一致、49 张表、MCP 表只声明一次")
    void theEngineUsesTheSameManagedSchemaArtifactTheControlPlaneApplies() {
        List<String> digests = digestsOf(read(MANIFEST));

        assertThat(digests).as("the manifest carries exactly one managed script").hasSize(1);
        assertThat(scriptMatches(digests.get(0)))
                .as("the script behind the manifest still has the recorded fingerprint")
                .isTrue();

        String script = read(MANAGED_SCRIPT);
        assertThat(countOccurrences(script, "CREATE TABLE public."))
                .as("both planes share the 49-table schema")
                .isEqualTo(49);
        assertThat(countOccurrences(script, "CREATE TABLE public." + PHYSICAL_TABLE + " ("))
                .as("the MCP task table is declared once for both planes")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("映射语句只按主键移动行、只读活跃行，且绝不落到物理分片名")
    void noStatementMovesRowsWithoutAPrimaryKeyAndNoQueryIgnoresTheSoftDeleteFilter() {
        List<String> dataPlane = statements(DATA_PLANE_MAPPER, null);
        List<String> reads = statements(DATA_PLANE_MAPPER, "select");
        List<String> writes = statements(DATA_PLANE_MAPPER, "update");

        assertThat(dataPlane).as("both reads and the versioned logical delete are mapped").hasSize(3);
        assertThat(reads).hasSize(2);
        assertThat(writes).hasSize(1);
        for (String body : reads) {
            assertThat(body).as("a guarded read never drops the soft-delete filter").contains("deleted_at IS NULL");
        }
        for (String body : writes) {
            assertThat(body).as("a bulk write by state alone is never mapped").contains("id = #{");
            assertThat(body).as("the logical delete carries the technical version CAS")
                    .contains("version = #{MP_OPTLOCK_VERSION_ORIGINAL}");
        }
        for (String body : dataPlane) {
            assertThat(body).contains(LOGICAL_TABLE);
            assertThat(body).as("no statement hard-codes the physical shard").doesNotContain(PHYSICAL_TABLE);
        }
        for (String body : statements(CONTROL_PLANE_MAPPER, null)) {
            assertThat(body).contains(LOGICAL_TABLE).doesNotContain(PHYSICAL_TABLE);
        }
        assertThat(read(ROW_MODEL))
                .as("the row model is mapped to the logical name only")
                .contains("@TableName(value = \"" + LOGICAL_TABLE + "\"")
                .doesNotContain(PHYSICAL_TABLE);
    }

    @Test
    @DisplayName("协议任务标识是不可翻译的字符串列，幂等键按租户组合")
    void theProtocolTaskIdentifierIsAnOpaqueStringColumnNotACoercedKey() {
        String block = tableBlock();

        assertThat(block)
                .as("the protocol identifier stays an opaque string column")
                .containsPattern("task_key\\s+varchar\\(64\\)\\s+NOT NULL");
        assertThat(block)
                .as("the request digest keeps its fixed width")
                .containsPattern("request_digest\\s+varchar\\(64\\)\\s+NOT NULL");
        assertThat(block)
                .as("the intent key is tenant-scoped")
                .contains("CONSTRAINT uq_mcp_task_instance_intent_1 UNIQUE (tenant_id, task_key)");
        assertThat(read(ROW_MODEL))
                .as("the row model never coerces the identifier")
                .contains("@TableField(\"task_key\")")
                .contains("private String taskKey;");
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("真实 Postgres 上的认领、CAS 与幂等反例")
    @EnabledIfEnvironmentVariable(
            named = "YUHENG_MANAGED_TEST_POSTGRES_URL",
            matches = ".+"
    )
    class OnPostgres {

        private final String jdbcUrl = environment("YUHENG_MANAGED_TEST_POSTGRES_URL");

        private final String user = environment("YUHENG_MANAGED_TEST_POSTGRES_USER");

        private final String password = environment("YUHENG_MANAGED_TEST_POSTGRES_PASSWORD");

        private final String schema = "yuheng_mcp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        private final Instant baseTime = Instant.parse("2026-09-22T16:30:00Z");

        private long lastId;

        @BeforeAll
        void applyTheSharedMcpTableIntoAFreshSchema() throws SQLException {
            try (Connection connection = open(); Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA " + quotedSchema());
                for (String ddl : translatedStatements()) {
                    statement.execute(ddl);
                }
            }
        }

        @AfterAll
        void dropTheAcceptanceSchema() throws SQLException {
            try (Connection connection = open(); Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + quotedSchema() + " CASCADE");
            }
        }

        @Test
        @DisplayName("同一租户的 task_key 不可重放，相邻租户互不影响")
        void aTaskKeyClaimedInsideATenantIsNeverReplayableAndAdjoiningTenantsStayIndependent() throws SQLException {
            String taskKey = "opaque-task-key-replay";
            insertTask(TENANT, taskKey, "WORKING", null, null, 0L);

            assertThatThrownBy(() -> insertTask(TENANT, taskKey, "WORKING", null, null, 0L))
                    .isInstanceOf(SQLException.class)
                    .extracting(McpTaskRecoveryPostgresqlIT::sqlStateOf)
                    .isEqualTo(UNIQUE_VIOLATION);

            long neighbour = insertTask(ADJOINING_TENANT, taskKey, "WORKING", null, null, 0L);
            assertThat(activeIdsOf(TENANT, taskKey))
                    .as("the tenant-scoped read finds only this tenant's row")
                    .hasSize(1);
            assertThat(activeIdsOf(ADJOINING_TENANT, taskKey)).containsExactly(neighbour);
        }

        @Test
        @DisplayName("逻辑删除让行退出所有受守卫读，但绝不释放幂等键")
        void logicalDeleteRemovesTheRowFromEveryGuardedReadButNeverFreesTheIntentKey() throws SQLException {
            String taskKey = "opaque-task-key-retired";
            long id = insertTask(TENANT, taskKey, "COMPLETED", null, null, 3L);
            long version = versionOf(id);

            assertThat(logicalDelete(id, version))
                    .as("the versioned logical delete hits once")
                    .isEqualTo(1);
            assertThat(activeIdsOf(id)).as("no guarded read can still see the deleted row").isEmpty();
            assertThatThrownBy(() -> insertTask(TENANT, taskKey, "WORKING", null, null, 0L))
                    .as("the intent key survives the soft delete: one key, one task, forever")
                    .isInstanceOf(SQLException.class)
                    .extracting(McpTaskRecoveryPostgresqlIT::sqlStateOf)
                    .isEqualTo(UNIQUE_VIOLATION);
            assertThat(logicalDelete(id, version))
                    .as("a second delete on the stale version affects no row and is never success")
                    .isZero();
        }

        @Test
        @DisplayName("只有持有未过期租约的占有者能续租")
        void onlyTheOwnerWithAnUnexpiredLeaseCanExtendTheLease() throws SQLException {
            Instant leaseUntil = baseTime.plusSeconds(30);
            long id = insertTask(TENANT, "opaque-task-key-lease", "WORKING", "worker-a", leaseUntil, 1L);

            assertThat(renew(id, "worker-b", baseTime.plusSeconds(5), leaseUntil.plusSeconds(30)))
                    .as("another owner never extends a lease it does not hold")
                    .isZero();
            assertThat(renew(id, "worker-a", leaseUntil, leaseUntil.plusSeconds(30)))
                    .as("at the expiry instant the lease no longer covers the task")
                    .isZero();
            assertThat(renew(id, "worker-b", leaseUntil.minusSeconds(1), leaseUntil.plusSeconds(30)))
                    .as("a live lease held by a peer still blocks that peer")
                    .isZero();
            assertThat(renew(id, "worker-a", baseTime.plusSeconds(5), leaseUntil.plusSeconds(30)))
                    .as("the holder inside the lease window wins")
                    .isEqualTo(1);
            assertThat(leaseUntilOf(id)).isEqualTo(leaseUntil.plusSeconds(30));
        }

        @Test
        @DisplayName("迁移必须精确匹配状态与业务 revision，命中才自增一次")
        void aTransitionRequiresTheExactStateAndBusinessRevisionAndMovesTheRevisionOnce() throws SQLException {
            long id =
                    insertTask(TENANT, "opaque-task-key-transition", "WORKING", "worker-a", baseTime.plusSeconds(60), 4L);

            assertThat(transition(id, "COMPLETED", 5L, baseTime.plusSeconds(10)))
                    .as("a state the row is not in never moves it")
                    .isZero();
            assertThat(transition(id, "WORKING", 3L, baseTime.plusSeconds(10)))
                    .as("a stale business revision never moves it")
                    .isZero();
            assertThat(transition(id, "WORKING", 4L, baseTime.plusSeconds(10)))
                    .as("the exact state plus revision pair moves it once")
                    .isEqualTo(1);
            assertThat(stateOf(id)).isEqualTo("COMPLETED");
            assertThat(revisionOf(id)).as("the business revision advanced exactly once").isEqualTo(5L);
            assertThat(transition(id, "WORKING", 4L, baseTime.plusSeconds(20)))
                    .as("replaying the same transition now affects no row")
                    .isZero();
        }

        @Test
        @DisplayName("读到同一技术 version 的两个工作者不可能都提交认领")
        void twoWorkersThatReadTheSameVersionCannotBothCommitTheirClaim() throws SQLException {
            long id = insertTask(TENANT, "opaque-task-key-claim", "WORKING", null, null, 0L);
            long version = versionOf(id);

            assertThat(claim(id, version, "worker-a", baseTime.plusSeconds(30)))
                    .as("the first compare-and-set commits")
                    .isEqualTo(1);
            assertThat(claim(id, version, "worker-b", baseTime.plusSeconds(60)))
                    .as("the loser of the same read never overwrites the winner")
                    .isZero();
            assertThat(workerOwnerOf(id)).isEqualTo("worker-a");
            assertThat(versionOf(id)).as("the technical version moved exactly once").isEqualTo(version + 1);
            assertThat(attemptCountOf(id)).as("the attempt counted the winning claim only").isEqualTo(1);
            assertThat(revisionOf(id)).isEqualTo(1L);
        }

        @Test
        @DisplayName("占有者与租约同生同灭，非法状态与超预算尝试计数被库拒绝")
        void theDatabaseRejectsAnImpossibleLeaseShapeStateAndAttemptBudget() throws SQLException {
            long id = insertTask(TENANT, "opaque-task-key-invariants", "WORKING", "worker-a", baseTime.plusSeconds(30), 1L);

            assertRejectedByTheTable("UPDATE gateway_mcp_task_instance_t0 SET lease_until = NULL WHERE id = ?", id);
            assertRejectedByTheTable("UPDATE gateway_mcp_task_instance_t0 SET worker_owner = NULL WHERE id = ?", id);
            assertRejectedByTheTable("UPDATE gateway_mcp_task_instance_t0 SET state = 'PAUSED' WHERE id = ?", id);
            assertRejectedByTheTable(
                    "UPDATE gateway_mcp_task_instance_t0 SET attempt_count = max_attempts + 1 WHERE id = ?", id);
            assertThatThrownBy(() -> insertTask(
                    TENANT, "opaque-task-key-digest", "WORKING", null, null, 0L, "not-a-digest"))
                    .as("the digest column really enforces its width instead of silently truncating")
                    .isInstanceOf(SQLException.class)
                    .extracting(McpTaskRecoveryPostgresqlIT::sqlStateOf)
                    .isEqualTo(CHECK_VIOLATION);
        }

        private void assertRejectedByTheTable(String sql, long id) {
            assertThatThrownBy(() -> update(sql, id))
                    .isInstanceOf(SQLException.class)
                    .extracting(McpTaskRecoveryPostgresqlIT::sqlStateOf)
                    .isEqualTo(CHECK_VIOLATION);
        }

        private List<Long> activeIdsOf(long tenantId, String taskKey) throws SQLException {
            return query("""
                    SELECT id FROM gateway_mcp_task_instance_t0
                     WHERE tenant_id = ? AND task_key = ? AND deleted_at IS NULL
                    """, tenantId, taskKey);
        }

        private List<Long> activeIdsOf(long id) throws SQLException {
            return query(
                    "SELECT id FROM gateway_mcp_task_instance_t0 WHERE id = ? AND deleted_at IS NULL", id);
        }

        private int logicalDelete(long id, long version) throws SQLException {
            return update("""
                    UPDATE gateway_mcp_task_instance_t0
                       SET deleted_at = ?, version = version + 1, update_time = ?
                     WHERE id = ? AND tenant_id = ? AND deleted_at IS NULL AND version = ?
                    """,
                    LocalDateTime.ofInstant(baseTime, ZoneOffset.UTC),
                    baseTime,
                    id,
                    TENANT,
                    version);
        }

        private int renew(long id, String workerOwner, Instant now, Instant newLeaseUntil) throws SQLException {
            return update("""
                    UPDATE gateway_mcp_task_instance_t0
                       SET lease_until = ?, update_time = ?
                     WHERE id = ? AND state = 'WORKING' AND worker_owner = ? AND lease_until > ?
                    """, newLeaseUntil, now, id, workerOwner, now);
        }

        private int transition(long id, String expectedState, long expectedRevision, Instant now) throws SQLException {
            return update("""
                    UPDATE gateway_mcp_task_instance_t0
                       SET state = 'COMPLETED', revision = revision + 1, update_time = ?
                     WHERE id = ? AND state = ? AND revision = ? AND deleted_at IS NULL
                    """, now, id, expectedState, expectedRevision);
        }

        private int claim(long id, long version, String workerOwner, Instant leaseUntil) throws SQLException {
            return update("""
                    UPDATE gateway_mcp_task_instance_t0
                       SET worker_owner = ?, lease_until = ?, attempt_count = attempt_count + 1,
                           revision = revision + 1, update_time = ?, version = version + 1
                     WHERE id = ? AND state = 'WORKING' AND deleted_at IS NULL AND version = ?
                    """, workerOwner, leaseUntil, leaseUntil, id, version);
        }

        private long insertTask(
                long tenantId,
                String taskKey,
                String state,
                String workerOwner,
                Instant leaseUntil,
                long revision) throws SQLException {
            return insertTask(tenantId, taskKey, state, workerOwner, leaseUntil, revision, "0".repeat(64));
        }

        private long insertTask(
                long tenantId,
                String taskKey,
                String state,
                String workerOwner,
                Instant leaseUntil,
                long revision,
                String requestDigest) throws SQLException {
            lastId++;
            long id = lastId;
            update("""
                    INSERT INTO gateway_mcp_task_instance_t0
                        (id, tenant_id, create_user_id, create_time, update_user_id, update_time, deleted_at, version,
                         principal_fingerprint, subject_id, client_id, server_code, tool_name, request_digest, state,
                         worker_owner, lease_until, execution_deadline, expires_at, attempt_count, max_attempts,
                         revision, task_key, subject_tenant_id)
                    VALUES (?, ?, ?, ?, ?, ?, NULL, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 3, ?, ?, ?)
                    """,
                    id,
                    tenantId,
                    "acceptance-user",
                    baseTime,
                    "acceptance-user",
                    baseTime,
                    "fingerprint-" + taskKey,
                    "subject-" + taskKey,
                    "client-" + taskKey,
                    "developer",
                    "acceptance.tool",
                    requestDigest,
                    state,
                    workerOwner,
                    leaseUntil,
                    baseTime.plusSeconds(300),
                    baseTime.plusSeconds(3600),
                    revision,
                    taskKey,
                    String.valueOf(tenantId));
            return id;
        }

        private int update(String sql, Object... parameters) throws SQLException {
            try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                return statement.executeUpdate();
            }
        }

        private List<Long> query(String sql, Object... parameters) throws SQLException {
            try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                List<Long> ids = new ArrayList<>();
                try (ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        ids.add(results.getLong(1));
                    }
                }
                return ids;
            }
        }

        private Object single(String sql, Object... parameters) throws SQLException {
            try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) {
                        throw new IllegalStateException("Missing acceptance row for " + sql);
                    }
                    return results.getObject(1);
                }
            }
        }

        private String stateOf(long id) throws SQLException {
            return String.valueOf(single("SELECT state FROM gateway_mcp_task_instance_t0 WHERE id = ?", id));
        }

        private String workerOwnerOf(long id) throws SQLException {
            return String.valueOf(single("SELECT worker_owner FROM gateway_mcp_task_instance_t0 WHERE id = ?", id));
        }

        private long revisionOf(long id) throws SQLException {
            return ((Number) single("SELECT revision FROM gateway_mcp_task_instance_t0 WHERE id = ?", id)).longValue();
        }

        private long versionOf(long id) throws SQLException {
            return ((Number) single("SELECT version FROM gateway_mcp_task_instance_t0 WHERE id = ?", id)).longValue();
        }

        private int attemptCountOf(long id) throws SQLException {
            return ((Number) single("SELECT attempt_count FROM gateway_mcp_task_instance_t0 WHERE id = ?", id))
                    .intValue();
        }

        private Instant leaseUntilOf(long id) throws SQLException {
            Object lease = single("SELECT lease_until FROM gateway_mcp_task_instance_t0 WHERE id = ?", id);
            return ((Timestamp) lease).toInstant();
        }

        private List<String> translatedStatements() {
            String target = quotedSchema() + "." + PHYSICAL_TABLE;
            List<String> statements = new ArrayList<>();
            statements.add(tableBlock().replace("public." + PHYSICAL_TABLE, target));
            Matcher indexes = INDEX_STATEMENT.matcher(read(MANAGED_SCRIPT));
            while (indexes.find()) {
                statements.add(indexes.group().replace("public." + PHYSICAL_TABLE, target));
            }
            assertThat(statements).as("the table plus its partial indexes come from the managed script")
                    .hasSize(SHARED_INDEX_COUNT + 1);
            return statements;
        }

        private Connection open() throws SQLException {
            return DriverManager.getConnection(jdbcUrl, user, password);
        }

        private String quotedSchema() {
            return "\"" + schema + "\"";
        }
    }

    private static String tableBlock() {
        String script = read(MANAGED_SCRIPT);
        int start = script.indexOf("CREATE TABLE public." + PHYSICAL_TABLE + " (");
        if (start < 0) {
            throw new IllegalStateException("The managed script no longer declares " + PHYSICAL_TABLE);
        }
        int end = script.indexOf("\n);", start);
        if (end < 0) {
            throw new IllegalStateException("Unbalanced declaration of " + PHYSICAL_TABLE);
        }
        return script.substring(start, end + 3);
    }

    private static List<String> declaredColumns() {
        List<String> columns = new ArrayList<>();
        for (String line : tableBlock().split("\n")) {
            Matcher column = Pattern.compile("^\\s{4}([a-z][a-z0-9_]*)\\s+[a-z]").matcher(line);
            if (column.find()) {
                columns.add(column.group(1));
            }
        }
        return columns;
    }

    private static List<String> mapperColumns(Path mapper) {
        Matcher projection =
                Pattern.compile("<sql id=\"columns\">([^<]+)</sql>").matcher(stripXmlComments(read(mapper)));
        if (!projection.find()) {
            throw new IllegalStateException("No column projection found in " + mapper);
        }
        List<String> columns = new ArrayList<>();
        for (String column : projection.group(1).split(",")) {
            columns.add(column.trim());
        }
        return columns;
    }

    private static List<String> statements(Path mapper, String tag) {
        List<String> bodies = new ArrayList<>();
        Matcher statement = MAPPED_STATEMENT.matcher(stripXmlComments(read(mapper)));
        while (statement.find()) {
            if (tag == null || tag.equals(statement.group(1))) {
                bodies.add(statement.group(3));
            }
        }
        return bodies;
    }

    private static List<String> digestsOf(String manifest) {
        List<String> digests = new ArrayList<>();
        Matcher declared = SHA256_ENTRY.matcher(manifest);
        while (declared.find()) {
            digests.add(declared.group(1));
        }
        return digests;
    }

    private static boolean scriptMatches(String declaredSha256) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(Files.readAllBytes(MANAGED_SCRIPT)))
                    .equals(declaredSha256);
        } catch (IOException | NoSuchAlgorithmException failure) {
            throw new IllegalStateException("Cannot fingerprint the managed script", failure);
        }
    }

    private static String stripXmlComments(String mapper) {
        return mapper.replaceAll("(?s)<!--.*?-->", "");
    }

    private static String sqlStateOf(Throwable failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof SQLException sqlFailure && sqlFailure.getSQLState() != null) {
                return sqlFailure.getSQLState();
            }
            cause = cause.getCause();
        }
        return "00000";
    }

    private static void bind(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int index = 0; index < parameters.length; index++) {
            Object parameter = parameters[index];
            if (parameter instanceof Instant instant) {
                statement.setObject(index + 1, LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
            } else {
                statement.setObject(index + 1, parameter);
            }
        }
    }

    private static String environment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing acceptance environment variable " + name);
        }
        return value;
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        for (int index = text.indexOf(needle); index >= 0; index = text.indexOf(needle, index + needle.length())) {
            count++;
        }
        return count;
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
