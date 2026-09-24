package top.egon.cola.component.yuheng.admin.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 中文说明：AI 平面（knowledge / llm / wiki 与 LLM engine）的横切合规闸口。这里不重复任何单点行为测试，只锁住修订后
 * 规范与强制 Java 规则在本平面内<b>整体</b>成立：受管 DDL 是唯一新增 SQL 且清单摘要就是字节摘要、组件 runner
 * 只有一个驱动者、AI 平面没有裸 JDBC/JPA/旧日期类型/事件出口、Bean 一律具名且限定注入、请求体一律过校验、枚举一律
 * 按 code 持久化并以 {@code @JsonValue} 出网、行模型从不过仓储边界、旧的引擎角色仍只有两种。
 * English summary: The cross-cutting conformance gate for the AI plane (knowledge / llm / wiki plus the LLM engine). It
 * restates no single-point behaviour test; it only pins that the revised specification and the mandated Java rules hold
 * <b>as a whole</b> inside this plane: the managed DDL is the only new SQL and its manifest checksum is the real byte
 * digest, exactly one class drives the component runner, the AI plane carries no raw JDBC/JPA/legacy date type/event
 * outlet, every bean is a named component with qualified injection, every request-body carrier is validated at the
 * handoff, every enum persists by code and projects through {@code @JsonValue}, no row model crosses the repository
 * boundary, and the legacy engine roles are still only the two.
 *
 * 用法 / Usage: 由 Plan Step 16 的 selector 执行；全部断言只读源码与资源文本，因此不需要容器、数据库或网络。
 * 任何一条失败都意味着平面引入了规范外的写法，必须在拥有它的文件里修正，而不是放宽本闸口。
 */
@DisplayName("AI 平面横切合规闸口")
class YuhengAiConformanceTest {

    /** 中文说明：本模块主源码根 / this module's main source root. */
    private static final Path ADMIN_MAIN = Path.of("src/main/java/top/egon/cola/component/yuheng/admin");

    /** 中文说明：本模块资源根 / this module's resource root. */
    private static final Path ADMIN_RESOURCES = Path.of("src/main/resources");

    /** 中文说明：engine 模块主源码根 / the engine module's main source root. */
    private static final Path ENGINE_MAIN = Path.of("../yuheng-llm-gateway/src/main/java");

    /** 中文说明：AI 平面在 admin 内的三个业务包 / the three AI-plane business packages inside admin. */
    private static final List<String> AI_PACKAGES = List.of("knowledge", "llm", "wiki");

    /** 中文说明：受管 DDL 的唯一脚本与清单 / the single managed-DDL script and its manifest. */
    private static final String MANAGED_SCRIPT = "db/egon-mp/20260922_001_yuheng_schema.sql";

    private static final String MANIFEST = "db/egon-mp/repository-manifest.json";

    /** 中文说明：受管脚本的文件名 / the managed script's file name. */
    private static final String MANAGED_SCRIPT_NAME = "20260922_001_yuheng_schema.sql";

    /** 中文说明：受管 DDL 声明版本 / the declared managed-DDL version. */
    private static final String SCHEMA_VERSION = "20260922_001";

    /** 中文说明：49 张逻辑表全部单分片落位 / every one of the 49 logical tables lands on one shard. */
    private static final int DECLARED_TABLE_COUNT = 49;

    /** 中文说明：MP 基线列 / the MyBatis-Plus baseline columns every managed table must carry. */
    private static final List<String> BASELINE_COLUMNS = List.of("id", "tenant_id", "create_user_id", "create_time",
            "update_user_id", "update_time", "deleted_at", "version");

    private static final Pattern CREATE_TABLE =
            Pattern.compile("CREATE TABLE public\\.(\\w+) \\((.*?)\\n\\);", Pattern.DOTALL);

    /** 中文说明：业务 Bean 注解（不含 {@code @Configuration}，它承载装配而非业务）/ business stereotypes, {@code @Configuration}
     * excluded because it carries wiring rather than business logic. */
    private static final Pattern STEREOTYPE =
            Pattern.compile("@(Service|RestController|Controller|Component|Repository)(?![A-Za-z])");

    /** 中文说明：带名字的 Bean 注解 / a stereotype that names the bean it contributes. */
    private static final Pattern NAMED_STEREOTYPE =
            Pattern.compile("@(Service|RestController|Controller|Component|Repository)(?![A-Za-z])\\s*\\(");

    /** 中文说明：声明在字段行上的注解窗口起点 / the start of an annotated declaration. */
    private static final Pattern FIELD_DECLARATION =
            Pattern.compile("^\\s+private final [\\w<>,\\.\\[\\] ]+? (\\w+);$");

    /** 中文说明：Rule 5 允许的工具与框架导入根 / the approved tooling and framework import roots (Rule 5). */
    private static final List<String> APPROVED_IMPORT_ROOTS = List.of("com.baomidou", "com.fasterxml", "io.swagger",
            "jakarta.annotation", "jakarta.servlet", "jakarta.validation", "java.", "javax.sql", "lombok.",
            "org.apache", "org.mapstruct", "org.mybatis", "org.reactivestreams", "org.slf4j", "org.springframework",
            "reactor.core", "top.egon");

    /**
     * 中文说明：合法的“无 {@code @Qualifier} 注入”白名单，每一项都在源码里有对应的双语理由注释：
     * {@code Map<String,T>} 是 Spring 的多元素按名注入，加限定符会把它收窄成单个 bean；{@code ConfigurableListableBeanFactory}
     * 由容器自身解析。白名单外的任何新增项都必须先立规范，不能在此就地放行。
     * English summary: The sanctioned qualifier-free injections, each backed by a bilingual rationale in the source:
     * {@code Map<String,T>} is Spring's by-name multi-element injection, so a qualifier would narrow it to one bean, and
     * {@code ConfigurableListableBeanFactory} is resolved by the container itself. Any future entry needs a Spec change
     * first; it must never be waved through here.
     */
    private static final Map<String, String> QUALIFIER_EXEMPT_FIELDS = Map.of(
            "LlmApiController#llmProtocolStrategies", "Spring multi-element Map<String,T> injection",
            "LlmPersistenceConfiguration#beanFactory", "container-resolvable BeanFactory introspection handle");

    @Test
    @DisplayName("受管 DDL 是唯一新增 SQL，且清单摘要就是最终字节的 SHA-256")
    void managedScriptIsTheOnlyNewSqlAndItsChecksumIsTheRealDigest() throws IOException {
        Map<String, String> manifest = readManifest();

        assertThat(sqlNames(ADMIN_RESOURCES.resolve("db/egon-mp")))
                .as("the managed family ships exactly one script").containsExactly(MANAGED_SCRIPT_NAME);
        assertThat(manifest).containsOnly(entry("version", SCHEMA_VERSION), entry("path", MANAGED_SCRIPT),
                entry("sha256", manifest.get("sha256")));
        assertThat(DigestUtils.sha256Hex(Files.readAllBytes(ADMIN_RESOURCES.resolve(MANAGED_SCRIPT))))
                .as("the manifest checksum is computed from the final SQL bytes, never guessed")
                .isEqualTo(manifest.get("sha256"));

        assertThat(sqlNames(ADMIN_RESOURCES.resolve("db/migration")))
                .as("the pre-managed V1-V13 history is retained untouched, with nothing appended")
                .hasSize(13)
                .allMatch(name -> name.matches("V(?:1|[2-9]|1[0-2])__.*\\.sql|V13__.*\\.sql"));
        assertThat(sqlNames(ADMIN_RESOURCES.resolve("db/migration")))
                .as("no legacy script was pulled back under management").doesNotContain(MANAGED_SCRIPT_NAME);
    }

    @Test
    @DisplayName("49 张受管表全部单分片落位并携带 MP 基线列")
    void everyManagedTableIsSingleShardAndCarriesTheMybatisPlusBaseline() throws IOException {
        Map<String, String> tables = tableBlocks();

        assertThat(tables).as("the managed script declares exactly the contracted table count")
                .hasSize(DECLARED_TABLE_COUNT);
        assertThat(tables.keySet()).as("every logical table lands on the single physical shard")
                .allMatch(name -> name.endsWith("_t0"));
        for (Map.Entry<String, String> table : tables.entrySet()) {
            for (String column : BASELINE_COLUMNS) {
                assertThat(table.getValue())
                        .as("%s declares the baseline column %s", table.getKey(), column)
                        .containsPattern(Pattern.compile("\\b" + column + "\\b\\s+\\w"));
            }
            assertThat(table.getValue()).as("%s keeps deleted_at microsecond-precise", table.getKey())
                    .containsPattern(Pattern.compile("deleted_at\\s+timestamp\\(6\\)"));
        }
    }

    @Test
    @DisplayName("软删唯一键一律把 deleted_at 组合进业务列（Rule 11）")
    void everyLifecycleUniqueKeyCombinesDeletedAtWithTheBusinessColumns() throws IOException {
        String sql = Files.readString(ADMIN_RESOURCES.resolve(MANAGED_SCRIPT), StandardCharsets.UTF_8);
        Pattern lifecycle = Pattern.compile("CONSTRAINT\\s+(\\w*life\\w*)\\s+UNIQUE\\s+\\(([^)]*)\\)", Pattern.DOTALL);

        List<String> missingDeletedAt = new ArrayList<>();
        int lifecycleKeys = 0;
        for (Map.Entry<String, String> table : tableBlocks().entrySet()) {
            Matcher matcher = lifecycle.matcher(table.getValue());
            while (matcher.find()) {
                lifecycleKeys++;
                if (!matcher.group(2).contains("deleted_at")) {
                    missingDeletedAt.add(table.getKey() + "." + matcher.group(1));
                }
            }
        }

        assertThat(lifecycleKeys).as("the managed schema declares soft-delete-aware unique keys").isGreaterThan(0);
        assertThat(missingDeletedAt).as("a lifecycle key without deleted_at would resurrect a soft-deleted row").isEmpty();
        assertThat(sql).as("active-side uniqueness is a partial index, never a nullable sentinel column")
                .contains("WHERE deleted_at IS NULL");
    }

    @Test
    @DisplayName("组件受管 DDL runner 只有 GatewayManagedDdlFactory 一个驱动者")
    void onlyTheManagedDdlFactoryDrivesTheComponentRunner() throws IOException {
        Set<String> drivers = new TreeSet<>();
        for (Map.Entry<Path, String> source : javaSources(Path.of("src/main/java")).entrySet()) {
            if (source.getValue().contains("EgonColaPostgreDdlRunner")) {
                drivers.add(source.getKey().getFileName().toString());
            }
        }

        assertThat(drivers).as("the runner is reached only through the approved extension point, never ad hoc")
                .containsExactly("GatewayManagedDdlConfiguration.java", "GatewayManagedDdlFactory.java");
    }

    @Test
    @DisplayName("AI 平面没有裸 JDBC/JPA/旧日期类型，也不读 ordinal")
    void aiPlaneCarriesNoRawJdbcNoJpaNoLegacyDateApiAndNoOrdinal() throws IOException {
        List<String> code = List.copyOf(aiPlaneSources().values());

        assertThat(code).as("the plane is persisted through MyBatis-Plus only")
                .noneMatch(source -> source.contains("import javax.persistence")
                        || source.contains("import org.springframework.jdbc")
                        || source.contains("import java.sql.")
                        || source.contains("JdbcTemplate"));
        assertThat(code).as("Rule 10: every date handle comes from java.time")
                .noneMatch(source -> source.contains("import java.util.Date")
                        || source.contains("import java.util.Calendar")
                        || source.contains("SimpleDateFormat"));
        assertThat(code).as("Rule 6: ordinal is never a business code")
                .noneMatch(source -> source.contains(".ordinal()"));
    }

    @Test
    @DisplayName("AI 平面枚举一律实现 EgonEnum、按 code 持久化并以 @JsonValue 出网")
    void aiPlaneEnumsPersistByCodeAndProjectThroughJsonValueNeverOrdinal() throws IOException {
        Map<String, String> declarations = new LinkedHashMap<>();
        for (Map.Entry<Path, String> source : aiPlaneSources().entrySet()) {
            if (source.getKey().getFileName().toString().endsWith("Enum.java")
                    && Pattern.compile("^enum\\s+\\w+|^public\\s+enum\\s+\\w+", Pattern.MULTILINE)
                            .matcher(source.getValue()).find()) {
                declarations.put(source.getKey().getFileName().toString(), source.getValue());
            }
        }

        assertThat(declarations).as("the gate would be vacuous without any enum in scope").isNotEmpty();
        for (Map.Entry<String, String> declaration : declarations.entrySet()) {
            String code = declaration.getValue();
            assertThat(code).as("%s must implement EgonEnum", declaration.getKey()).contains("implements EgonEnum");
            assertThat(countOccurrences(code, "@JsonValue")).as("%s exposes exactly one wire value", declaration.getKey())
                    .isEqualTo(1);
            assertThat(countOccurrences(code, "@EnumValue")).as("%s persists at least one code", declaration.getKey())
                    .isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    @DisplayName("业务 Bean 具名、@Slf4j、@RequiredArgsConstructor 且逐字段 @Qualifier（Rule 4）")
    void springBeansAreNamedSlf4jAnnotatedAndInjectQualifiedCollaborators() throws IOException {
        for (Map.Entry<Path, String> source : aiPlaneSources().entrySet()) {
            String code = source.getValue();
            if (!STEREOTYPE.matcher(code).find()) {
                continue;
            }
            String file = source.getKey().getFileName().toString();
            List<String> lines = List.of(code.split("\n", -1));

            assertThat(countMatches(code, NAMED_STEREOTYPE))
                    .as("%s must name every bean it contributes", file).isEqualTo(countMatches(code, STEREOTYPE));
            assertThat(code).as("%s injects its logger through @Slf4j", file).contains("@Slf4j");
            Map<String, Integer> injected = injectedFieldLines(lines);
            if (!injected.isEmpty()) {
                assertThat(code).as("%s declares its constructor with @RequiredArgsConstructor", file)
                        .contains("@RequiredArgsConstructor");
            }
            for (Map.Entry<String, Integer> field : injected.entrySet()) {
                String key = file.replace(".java", "") + "#" + field.getKey();
                if (QUALIFIER_EXEMPT_FIELDS.containsKey(key)) {
                    continue;
                }
                assertThat(annotationWindow(lines, field.getValue()))
                        .as("%s must be injected with @Qualifier (Rule 4)", key).contains("@Qualifier");
            }
        }
    }

    @Test
    @DisplayName("每个请求体载体都在入口处被校验，复用对象带分组（Rule 2）")
    void everyRequestBodyCarrierIsValidatedAtTheHandoff() throws IOException {
        int carriers = 0;
        for (Map.Entry<Path, String> source : aiPlaneSources().entrySet()) {
            List<String> lines = List.of(source.getValue().split("\n", -1));
            for (int index = 0; index < lines.size(); index++) {
                if (!lines.get(index).contains("@RequestBody")) {
                    continue;
                }
                carriers++;
                assertThat(annotationWindow(lines, index) + lines.get(index))
                        .as("%s must validate its request-body carrier", source.getKey().getFileName())
                        .containsPattern(Pattern.compile("@(Valid|Validated)(?![A-Za-z])"));
            }
        }

        assertThat(carriers).as("the AI plane exposes request-body commands to validate").isGreaterThanOrEqualTo(9);
    }

    @Test
    @DisplayName("AI 平面只使用批准的工具与框架导入根（Rule 5）")
    void aiPlaneImportsStayWithinTheApprovedTooling() throws IOException {
        Map<String, Set<String>> offenders = new LinkedHashMap<>();
        for (Map.Entry<Path, String> source : aiPlaneSources().entrySet()) {
            Matcher matcher = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.]+)\\s*;", Pattern.MULTILINE)
                    .matcher(source.getValue());
            while (matcher.find()) {
                String imported = matcher.group(1);
                if (APPROVED_IMPORT_ROOTS.stream().noneMatch(imported::startsWith)) {
                    offenders.computeIfAbsent(imported, key -> new LinkedHashSet<>())
                            .add(source.getKey().getFileName().toString());
                }
            }
        }

        assertThat(offenders).as("no second JSON/date/collection utility stack may enter the plane").isEmpty();
    }

    @Test
    @DisplayName("新 AI 平面不发事件，也不带来 Outbox/MQ 依赖（修订 §13）")
    void aiPlaneEmitsNoEventsAndCarriesNoOutboxOrMessagingDependency() throws IOException {
        Map<Path, String> scope = aiPlaneSources();

        assertThat(scope.keySet()).as("no event carrier is declared in the plane")
                .noneMatch(path -> path.getFileName().toString().endsWith("Event.java"));
        assertThat(scope.values()).as("no messaging or outbox client is imported")
                .noneMatch(code -> code.contains("Outbox") || code.contains("KafkaTemplate")
                        || code.contains("RocketMQ") || code.contains("MessageChannel"));
        for (String pom : List.of("pom.xml", "../yuheng-llm-gateway/pom.xml")) {
            assertThat(Files.readString(Path.of(pom), StandardCharsets.UTF_8))
                    .as("%s must not gain an outbox dependency for the AI plane", pom)
                    .doesNotContain("transactional-outbox");
        }
    }

    @Test
    @DisplayName("行模型从不出仓储层，控制器与服务层也看不到 DAO（MC-MODEL-001）")
    void rowModelsNeverCrossTheRepositoryBoundary() throws IOException {
        Pattern rowModel = Pattern.compile("^\\s*import\\s+[\\w.]+\\.\\w+(PO|DAO)\\s*;", Pattern.MULTILINE);

        for (Map.Entry<Path, String> source : aiPlaneSources().entrySet()) {
            String layer = layerOf(source.getKey());
            if (!Set.of("domain", "repository", "converter", "dao").contains(layer)) {
                assertThat(rowModel.matcher(source.getValue()).find())
                        .as("%s sits in the %s layer and must reach the database through a repository port",
                                source.getKey().getFileName(), layer)
                        .isFalse();
            }
        }
    }

    @Test
    @DisplayName("旧发布平面的引擎角色仍然只有两种，LLM 不入 journal")
    void legacyEngineRolesStillCoverOnlyTheTwoPublicationPlanes() throws IOException {
        String code = stripComments(Files.readString(Path.of(
                "../yuheng-contract/src/main/java/top/egon/cola/component/yuheng/contract/runtime/GatewayEngineRoleEnum.java"),
                StandardCharsets.UTF_8));

        assertThat(constantsOf(code)).as("the legacy publication journal still knows exactly two engine roles")
                .containsExactly("API_RPC", "MCP");
        assertThat(code).as("adding an LLM engine role would silently widen the legacy release contract")
                .doesNotContain("LLM");
    }

    @Test
    @DisplayName("协议分发是一张策略注册表，不是条件分支（Rule 9）")
    void protocolDispatchIsAStrategyRegistryRatherThanConditionalBranching() throws IOException {
        List<String> strategies = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(ENGINE_MAIN)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().endsWith("ProtocolStrategy.java"))
                    .sorted(Comparator.naturalOrder()).collect(Collectors.toList())) {
                if (STEREOTYPE.matcher(stripComments(Files.readString(path, StandardCharsets.UTF_8))).find()) {
                    strategies.add(path.getFileName().toString());
                }
            }
        }

        assertThat(strategies).as("one named adapter per protocol").hasSize(4);
        String controller = stripComments(Files.readString(
                ENGINE_MAIN.resolve("top/egon/cola/component/yuheng/llm/proxy/controller/LlmApiController.java"),
                StandardCharsets.UTF_8));
        assertThat(controller).as("the orchestrator holds the protocol-to-bean registry").contains("llmProtocolStrategyRegistry");
        assertThat(controller).as("and never branches on a protocol").doesNotContain("switch");
    }

    private Map<Path, String> aiPlaneSources() throws IOException {
        Map<Path, String> sources = new LinkedHashMap<>();
        for (String businessPackage : AI_PACKAGES) {
            sources.putAll(javaSources(ADMIN_MAIN.resolve(businessPackage)));
        }
        for (Map.Entry<Path, String> candidate : javaSources(ADMIN_MAIN.resolve("config")).entrySet()) {
            if (candidate.getKey().getFileName().toString().startsWith("GatewayManagedDdl")) {
                sources.put(candidate.getKey(), candidate.getValue());
            }
        }
        sources.putAll(javaSources(ENGINE_MAIN));
        return sources;
    }

    private Map<Path, String> javaSources(Path root) throws IOException {
        Map<Path, String> sources = new LinkedHashMap<>();
        if (!Files.isDirectory(root)) {
            return sources;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> java = paths.filter(p -> p.toString().endsWith(".java")).sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
            for (Path path : java) {
                sources.put(path, stripComments(Files.readString(path, StandardCharsets.UTF_8)));
            }
        }
        return sources;
    }

    private Map<String, String> tableBlocks() throws IOException {
        String sql = Files.readString(ADMIN_RESOURCES.resolve(MANAGED_SCRIPT), StandardCharsets.UTF_8);
        Map<String, String> tables = new LinkedHashMap<>();
        Matcher matcher = CREATE_TABLE.matcher(sql);
        while (matcher.find()) {
            tables.put(matcher.group(1), matcher.group(2));
        }
        return tables;
    }

    /** 中文说明：清单里只关心版本/路径/摘要三项 / the manifest fields this gate pins. */
    private Map<String, String> readManifest() throws IOException {
        String json = Files.readString(ADMIN_RESOURCES.resolve(MANIFEST), StandardCharsets.UTF_8);
        assertThat(json).as("the managed family stays the web family").contains("\"family\"");
        assertThat(countOccurrences(json, "\"path\"")).as("exactly one managed script").isEqualTo(1);
        Map<String, String> manifest = new LinkedHashMap<>();
        for (String key : List.of("version", "path", "sha256")) {
            Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
            assertThat(matcher.find()).as("the manifest declares %s", key).isTrue();
            manifest.put(key, matcher.group(1));
        }
        assertThat(json.substring(json.indexOf("\"family\""))).as("family is web").contains("web");
        return manifest;
    }

    /**
     * 中文说明：注释与字符串剥离，避免把双语说明里提到的 JdbcTemplate/ordinal/Date 字样当成代码事实。
     * English summary: Comments and string literals are stripped so a bilingual rationale that mentions
     * {@code JdbcTemplate}, {@code ordinal()} or {@code Date} never reads as code.
     */
    private static String stripComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int index = 0;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (current == '"') {
                if (source.startsWith("\"\"\"", index)) {
                    int end = source.indexOf("\"\"\"", index + 3);
                    out.append(' ');
                    index = end < 0 ? source.length() : end + 3;
                    continue;
                }
                index = skipLiteral(source, index, '"');
                out.append("\"literal\"");
            } else if (current == '\'') {
                index = skipLiteral(source, index, '\'');
                out.append("'c'");
            } else if (source.startsWith("//", index)) {
                int end = source.indexOf('\n', index);
                out.append('\n');
                index = end < 0 ? source.length() : end;
            } else if (source.startsWith("/*", index)) {
                int end = source.indexOf("*/", index + 2);
                out.append('\n');
                index = end < 0 ? source.length() : end + 2;
            } else {
                out.append(current);
                index++;
            }
        }
        return out.toString();
    }

    private static int skipLiteral(String source, int start, char quote) {
        int index = start + 1;
        while (index < source.length() && source.charAt(index) != quote) {
            index += source.charAt(index) == '\\' ? 2 : 1;
        }
        return index + 1;
    }

    /** 中文说明：外层类体（深度 1）里声明、且没有初始化器的实例字段，即真正的容器注入点。 */
    private static Map<String, Integer> injectedFieldLines(List<String> lines) {
        Map<String, Integer> fields = new LinkedHashMap<>();
        int depth = 0;
        for (int index = 0; index < lines.size(); index++) {
            Matcher matcher = FIELD_DECLARATION.matcher(lines.get(index));
            if (depth == 1 && matcher.matches()) {
                fields.put(matcher.group(1), index);
            }
            depth += countOccurrences(lines.get(index), "{") - countOccurrences(lines.get(index), "}");
        }
        return fields;
    }

    /** 中文说明：某一行上方连续的注解行（跨过被剥离注释留下的空行），直到上一条语句结束。 */
    private static String annotationWindow(List<String> lines, int index) {
        StringBuilder window = new StringBuilder();
        for (int cursor = index - 1; cursor >= 0; cursor--) {
            String previous = lines.get(cursor).strip();
            if (previous.isEmpty()) {
                continue;
            }
            if (previous.startsWith("@")) {
                window.insert(0, previous + "\n");
                continue;
            }
            break;
        }
        return window.toString();
    }

    private static List<String> constantsOf(String enumSource) {
        List<String> constants = new ArrayList<>();
        Matcher matcher = Pattern.compile("^\\s{4}([A-Z][A-Z0-9_]*)\\s*[;,({]", Pattern.MULTILINE).matcher(enumSource);
        while (matcher.find()) {
            constants.add(matcher.group(1));
        }
        return constants;
    }

    private static List<String> sqlNames(Path root) throws IOException {
        try (Stream<Path> paths = Files.list(root)) {
            return paths.map(path -> path.getFileName().toString()).filter(name -> name.endsWith(".sql")).sorted()
                    .collect(Collectors.toList());
        }
    }

    private static String layerOf(Path path) {
        String normalized = path.toString().replace('\\', '/');
        for (String layer : List.of("controller", "service", "repository", "converter", "domain", "dao", "scheduled",
                "validation")) {
            if (normalized.contains("/" + layer + "/")) {
                return layer;
            }
        }
        return "other";
    }

    private static int countMatches(String code, Pattern pattern) {
        Matcher matcher = pattern.matcher(code);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static int countOccurrences(String code, String fragment) {
        int count = 0;
        for (int index = code.indexOf(fragment); index >= 0; index = code.indexOf(fragment, index + fragment.length())) {
            count++;
        }
        return count;
    }
}
