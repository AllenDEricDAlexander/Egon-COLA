package top.egon.cola.component.yuheng.admin.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlManifestBO;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.yuheng.admin.config.GatewayManagedDdlConfiguration;
import top.egon.cola.component.yuheng.admin.config.GatewayManagedDdlFactory;
import top.egon.cola.component.yuheng.admin.config.GatewayPersistenceProperties;

/**
 * 中文说明：{@code GatewayManagedDdlConfigurationTest} 固定 Step 9 的切换合同：MyBatis-Plus 是唯一数据源/TM，
 * 装配中不得再残留 JPA/Flyway/ORM 运行期类型；空库基线必须把 yuheng 各模块声明的<b>每一个</b> {@code @TableName}
 * 落成恰好一张 {@code *_t0} 物理表（合计 49 张）、自带 {@code ddl_history}，且不含组件 runner 禁止的
 * {@code DROP}/事务控制/{@code CONCURRENTLY} 语句；manifest 的 family 只能取组件允许的六个值之一（本项目为
 * {@code web}），其 sha256 必须等于<b>最终 SQL 字节</b>的摘要——绝不为了通过检查而改哈希；期望版本或摘要与
 * manifest 不一致时，工厂必须在触达拓扑与任何连接动作<b>之前</b>失败关闭。
 * English summary: {@code GatewayManagedDdlConfigurationTest} pins the Step 9 cutover contract: MyBatis-Plus is the only
 * datasource/transaction manager and no JPA, Flyway or ORM runtime type may remain in the wiring; the empty-database
 * baseline must materialise <b>every</b> {@code @TableName} declared across the yuheng modules as exactly one
 * {@code *_t0} physical table (49 in total), ship its own {@code ddl_history}, and contain no statement the component
 * runner forbids ({@code DROP}, transaction control, {@code CONCURRENTLY}); the manifest family must be one the component
 * accepts ({@code web} here) and its sha256 must equal the digest of the <b>final SQL bytes</b> — the hash is never
 * edited to force a pass; and a mismatching expected version or checksum must fail closed <b>before</b> the factory
 * touches the topology or any connection.
 *
 * 用法 / Usage: 纯源码/资源合同测试，不启动 Spring Boot 上下文、Docker 或数据库（与同目录架构测试一致）；
 * 它把 49 表基线与 MP 模型双向对齐，但真实 MyBatis→guard→ShardingSphere→PG 建表和空库 ready 判定仍属运行期待证。
 */
class GatewayManagedDdlConfigurationTest {

    private static final String SQL_RESOURCE = "db/egon-mp/20260922_001_yuheng_schema.sql";
    private static final String MANIFEST_RESOURCE = "db/egon-mp/repository-manifest.json";
    private static final String EXPECTED_VERSION = "20260922_001";
    private static final String TECHNICAL_TABLE = "ddl_history";
    private static final Pattern CREATE_TABLE =
            Pattern.compile("CREATE TABLE (?:IF NOT EXISTS )?(?:[a-z_][a-z0-9_]*\\.)?([a-z][a-z0-9_]*)",
                    Pattern.CASE_INSENSITIVE);
    // 真实模型一律写成 @TableName(value = "…", autoResultMap = true)，简写形式也必须支持。
    // Every row model spells it out as @TableName(value = "…", autoResultMap = true); the short form stays accepted too.
    private static final Pattern TABLE_NAME_ANNOTATION =
            Pattern.compile("@TableName\\(\\s*(?:value\\s*=\\s*)?\"([a-z][a-z0-9_]*)\"");
    private static final List<String> MANAGED_COLUMNS = List.of(
            "tenant_id", "deleted_at", "version", "create_time", "update_time", "create_user_id", "update_user_id");

    private static ValidatorFactory validatorFactory;
    private static String baselineSql;
    private static Set<String> mappedTables;

    @BeforeAll
    static void loadBaseline() throws IOException {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        baselineSql = readResource(SQL_RESOURCE);
        mappedTables = collectMappedTables();
    }

    @AfterAll
    static void releaseValidator() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("基线把每个MP模型落成恰好一张_t0节点表")
    void baselineMaterialisesEveryMappedModelAsOnePhysicalNode() {
        Set<String> baselineTables = tablesIn(baselineSql);

        assertThat(mappedTables).hasSize(49);
        assertThat(baselineTables).hasSize(50).contains(TECHNICAL_TABLE);
        assertThat(baselineTables).filteredOn(name -> !TECHNICAL_TABLE.equals(name))
                .allMatch(name -> name.endsWith("_t0"), "every business table must use the _t0 node suffix");
        assertThat(baselineTables)
                .containsExactlyInAnyOrderElementsOf(unionOfBusinessNodes(mappedTables));
    }

    @Test
    @DisplayName("每张表都携带MP租户、软删、版本与审计列")
    void everyTableCarriesTheManagedColumnSet() {
        for (String fragment : baselineSql.split("(?=CREATE TABLE )")) {
            Set<String> named = tablesIn(fragment);
            // ddl_history 是 runner 自己的技术表，列集合由组件固定，不受 EgonModel 列规范约束。
            // ddl_history is the runner's own technical table; its column set is fixed by the component and is not an
            // EgonModel projection, so it is excluded from the managed-column rule.
            if (named.isEmpty() || TECHNICAL_TABLE.equals(named.iterator().next())) {
                continue;
            }
            assertThat(fragment).as("managed columns of %s", named.iterator().next())
                    .contains(MANAGED_COLUMNS.toArray(String[]::new));
        }
    }

    @Test
    @DisplayName("基线自带ddl_history且不含runner禁止的语句")
    void baselineIsRunnerLegal() {
        assertThat(baselineSql).contains(TECHNICAL_TABLE);
        // 受管runner只禁止「顶层」事务控制；DO $egon$ ... $egon$; 体内的 BEGIN/END 是 plpgsql 语法，必须先按组件同样的方式遮蔽。
        // The runner bans TOP-LEVEL transaction control only; BEGIN/END inside a dollar-quoted DO body is plpgsql syntax,
        // so the body is masked first exactly like the component does.
        String topLevel = baselineSql.replaceAll("(?is)DO\\s+\\$egon\\$.*?\\$egon\\$;", "");
        assertThat(baselineSql)
                .doesNotContainIgnoringCase("drop table")
                .doesNotContainIgnoringCase("drop index")
                .doesNotContainIgnoringCase("truncate")
                .doesNotContainIgnoringCase("concurrently")
                .doesNotContainPattern("(?im)^\\s*(insert|update|delete)\\b");
        assertThat(topLevel)
                .doesNotContainPattern("(?im)^\\s*(begin|commit|rollback)\\b");
        // 主体只在本部署被钉死的 SHARD 角色下建表，其余角色 fail closed；历史表必须留在角色分支之外。
        // The body creates tables only under the SHARD role this deployment pins, raising for any other role; the history
        // table must stay outside the role branch because the runner inserts into it for every target.
        assertThat(baselineSql).contains("current_setting('egon_migration.role') = 'SHARD'");
        assertThat(baselineSql).contains("RAISE EXCEPTION");
        assertThat(topLevel).contains("CREATE TABLE " + TECHNICAL_TABLE);
    }

    @Test
    @DisplayName("manifest摘要取自最终SQL字节且资源只命中一份")
    void manifestChecksumIsComputedFromFinalSqlBytes() throws IOException {
        EgonColaDdlManifestBO manifest =
                new ObjectMapper().readValue(readResource(MANIFEST_RESOURCE), EgonColaDdlManifestBO.class);
        assertThat(manifest.family()).isEqualTo("web");
        assertThat(manifest.scripts()).hasSize(1);
        EgonColaDdlManifestBO.ScriptBO script = manifest.scripts().get(0);
        assertThat(script.version()).isEqualTo(EXPECTED_VERSION);
        assertThat(script.path()).isEqualTo(SQL_RESOURCE);

        Resource[] matches = new PathMatchingResourcePatternResolver().getResources("classpath*:" + SQL_RESOURCE);
        assertThat(matches).as("the SQL must resolve to exactly one classpath resource").hasSize(1);
        assertThat(HexFormat.of().formatHex(sha256(readResource(SQL_RESOURCE).getBytes(StandardCharsets.UTF_8))))
                .isEqualTo(script.sha256());
    }

    @Test
    @DisplayName("期望版本与manifest不符时在拓扑与连接之前失败关闭")
    void expectedSchemaMismatchFailsBeforeTopologyOrConnection() {
        GatewayPersistenceProperties stale = new GatewayPersistenceProperties(
                9001L, 9002L, "yuheng-admin-service", "19700101_001", "0".repeat(64), true);

        // 拓扑校验器与 runner 传 null：一旦实现越过去触碰它们就会抛 NPE，从而证明错配确实在最早处被拦下。
        // The validator and runner are null: reaching either would raise an NPE, proving the mismatch is caught first.
        assertThatThrownBy(() -> new GatewayManagedDdlFactory(
                null, new ObjectMapper(), stale, null, null).create(Map.of(), new byte[0]))
                .isInstanceOf(EgonColaMybatisPlusConfigurationException.class)
                .hasMessageContaining("SCHEMA_MANIFEST_MISMATCH");
    }

    @Test
    @DisplayName("部署身份缺任一项即失败且没有默认租户1")
    void deploymentIdentityFailsClosedWithoutAFallbackTenant() {
        Validator validator = validatorFactory.getValidator();

        Set<ConstraintViolation<GatewayPersistenceProperties>> absent = validator.validate(
                new GatewayPersistenceProperties(null, null, " ", null, null, true));
        assertThat(absent).hasSize(5);

        assertThat(validator.validate(new GatewayPersistenceProperties(
                0L, 0L, "yuheng-admin-service", EXPECTED_VERSION, "not-a-sha", false)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("tenantId", "identityTenantId", "expectedSchemaSha256");
    }

    @Test
    @DisplayName("受管DDL只覆盖组件扩展点名且开关必须显式打开")
    void configurationOverridesOnlyTheComponentExtensionPoint() {
        List<Bean> beans = new ArrayList<>();
        List<ConditionalOnProperty> conditions = new ArrayList<>();
        for (var method : GatewayManagedDdlConfiguration.class.getDeclaredMethods()) {
            if (method.getAnnotation(Bean.class) != null) {
                beans.add(method.getAnnotation(Bean.class));
                conditions.add(method.getAnnotation(ConditionalOnProperty.class));
            }
        }
        assertThat(beans).hasSize(1);
        assertThat(beans.get(0).value()).containsExactly("egonColaShardingLogicalDataSourceFactory");
        assertThat(conditions.get(0).prefix()).isEqualTo("yuheng.persistence");
        assertThat(conditions.get(0).name()).containsExactly("managed-ddl-enabled");
        assertThat(conditions.get(0).havingValue()).isEqualTo("true");
        assertThat(GatewayManagedDdlConfiguration.class.getAnnotation(Configuration.class).proxyBeanMethods())
                .isFalse();
    }

    @Test
    @DisplayName("装配源码不再残留JPA、Flyway与ORM运行期类型")
    void noLegacyOrmOrMigrationRuntimeRemains() throws IOException {
        // 只禁止「代码位置」上的残留：注释里说明「JPA/Flyway 已被移除」是必要的文档，不能被这条断言误伤。
        // Only code positions are banned: a comment explaining that the JPA/Flyway runtime was removed is necessary
        // documentation and must not be caught by this assertion.
        Map<String, Pattern> forbidden = Map.of(
                "JPA API", Pattern.compile("(?m)^\\s*import\\s+(?:static\\s+)?jakarta\\.persistence\\."),
                "Spring Data JPA", Pattern.compile("(?m)^\\s*import\\s+(?:static\\s+)?org\\.springframework\\.data\\.jpa\\."),
                "Spring ORM", Pattern.compile("(?m)^\\s*import\\s+(?:static\\s+)?org\\.springframework\\.orm\\."),
                "Flyway", Pattern.compile("(?m)^\\s*import\\s+(?:static\\s+)?org\\.flywaydb\\."),
                "JPA entity scan", Pattern.compile("(?m)^\\s*@EntityScan\\b"),
                "JPA repositories", Pattern.compile("(?m)^\\s*@EnableJpaRepositories\\b"));
        List<String> offenders = new ArrayList<>();
        for (Path source : yuhengMainSources()) {
            String text = Files.readString(source, StandardCharsets.UTF_8);
            forbidden.forEach((label, pattern) -> {
                if (pattern.matcher(text).find()) {
                    offenders.add(source + " -> " + label);
                }
            });
        }
        assertThat(offenders).isEmpty();
    }

    private static Set<String> collectMappedTables() throws IOException {
        Set<String> tables = new LinkedHashSet<>();
        for (Path source : yuhengMainSources()) {
            Matcher matcher = TABLE_NAME_ANNOTATION.matcher(Files.readString(source, StandardCharsets.UTF_8));
            while (matcher.find()) {
                tables.add(matcher.group(1));
            }
        }
        return tables;
    }

    private static List<Path> yuhengMainSources() throws IOException {
        List<Path> sources = new ArrayList<>();
        try (Stream<Path> moduleDirectories = Files.list(Path.of(".."))) {
            for (Path module : moduleDirectories.filter(Files::isDirectory).toList()) {
                Path sourceRoot = module.resolve("src/main/java");
                if (Files.isDirectory(sourceRoot)) {
                    try (Stream<Path> walked = Files.walk(sourceRoot)) {
                        walked.filter(path -> path.toString().endsWith(".java")).forEach(sources::add);
                    }
                }
            }
        }
        return sources;
    }

    private static Set<String> tablesIn(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        Matcher matcher = CREATE_TABLE.matcher(sql);
        while (matcher.find()) {
            tables.add(matcher.group(1).toLowerCase());
        }
        return tables;
    }

    private static Set<String> unionOfBusinessNodes(Set<String> logicalTables) {
        Set<String> nodes = new LinkedHashSet<>();
        logicalTables.forEach(table -> nodes.add(table + "_t0"));
        nodes.add(TECHNICAL_TABLE);
        return nodes;
    }

    private static String readResource(String resource) {
        try (InputStream input = GatewayManagedDdlConfigurationTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            assertThat(input).as("classpath resource %s", resource).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static byte[] sha256(byte[] content) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(content);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
