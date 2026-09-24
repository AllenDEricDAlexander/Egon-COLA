package top.egon.cola.component.yuheng.admin.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;

/**
 * 中文说明：{@code GatewayProfileParityTest} 把 Rule 7「多环境配置文件必须保持配置一致，但值不一定一致」落到本 Plan
 * 的三个进程（Admin / MCP 数据面 / LLM 网关）上，口径是<b>键</b>而不是值：（一）三个进程必须绑定同一个部署身份前缀、
 * 同一组身份键与同一套约束（租户、受信任身份租户、服务审计主体、被期望的受管 schema 版本/指纹、受管 DDL 角色开关），
 * 任何一个进程偷偷少一个键或多一个默认值都会在这里失败——因为「配置不一致」在共享同一套受管表时等于跨进程租户错配；
 * （二）这些部署身份键在任何 profile 文档里都不许出现，它们只能由运维在启动时提供，缺值即绑定失败而不是退化成共享租户；
 * （三）同一进程的基础文档与环境覆盖文档在 AI 面前缀下必须键结构一致；（四）受管 schema 的版本与指纹只有一个事实来源
 * （仓库清单），且任何主源码都不许把版本号或 64 位十六进制指纹写死；（五）跨进程读写租户时使用的 MDC 键必须与组件
 * 默认键同源。用户明确要求「配置先不写，启动时补充」，因此这里断言的是键的合同与缺省即失败的边界，不校验具体值。
 * English summary: {@code GatewayProfileParityTest} applies Rule 7 — "multi-environment configuration files must stay
 * consistent in configuration, though not necessarily in values" — to the three processes this Plan ships (Admin, the
 * MCP data plane and the LLM gateway), and it asserts <b>keys</b> rather than values: all three must bind one
 * deployment-identity prefix with one identical key set and identical constraints (tenant, trusted identity tenant,
 * service audit principal, expected managed schema version/fingerprint, managed-DDL role flag), because a diverging key
 * in a process that shares the same managed tables is a cross-process tenancy mismatch; those identity keys must appear
 * in no profile document at all, since only the operator supplies them at startup and a missing value has to fail
 * binding rather than degrade into a shared tenant; within one process the base document and each environment overlay
 * must agree on the key structure under the AI-plane prefixes; the managed schema's version and fingerprint have exactly
 * one source of truth (the repository manifest) and no main source may hard-code either the version or a 64-hex
 * fingerprint; and the MDC keys used to carry tenancy must share one origin with the component defaults. The user asked
 * for configuration to be supplied at startup, so this asserts key contracts and fail-closed boundaries, not concrete
 * values.
 *
 * 用法 / Usage: 源码级与文档级门禁；只读三个模块的 {@code src/main} 与其 profile 文档，不启动 Spring 上下文、不连库、
 * 不开容器。/ A source- and document-level gate that only reads the three modules' {@code src/main} trees and profile
 * documents; no Spring context, database or container is started.
 */
class GatewayProfileParityTest {

    private static final ObjectMapper MANIFEST_MAPPER = new ObjectMapper();

    /** 中文说明：三个受管进程及其部署身份绑定类的相对位置。 English summary: the three managed processes and where their deployment-identity binding classes live. */
    private static final Map<String, String> IDENTITY_BINDINGS = Map.of(
            "yuheng-admin", "src/main/java/top/egon/cola/component/yuheng/admin/config/GatewayPersistenceProperties.java",
            "yuheng-mcp-gateway",
            "src/main/java/top/egon/cola/component/yuheng/mcp/engine/config/McpPersistenceProperties.java",
            "yuheng-llm-gateway", "src/main/java/top/egon/cola/component/yuheng/llm/config/LlmPersistenceProperties.java");

    /** 中文说明：本 Plan 的 AI 面配置前缀，基础文档与环境覆盖文档必须在这些前缀下保持键一致。
     * {@code egon.cola.component.yuheng.*}（Tianshu 注册与上报）不属于本 Plan 的合同面，其 local 覆盖差异已登记为审计发现。
     * English summary: the AI-plane prefixes whose key structure must agree between a base document and its overlays.
     * {@code egon.cola.component.yuheng.*} (Tianshu registration and reporting) is outside this Plan's contract surface,
     * and its local-overlay divergence is registered as an audit finding. */
    private static final List<String> AI_PREFIXES = List.of(
            "yuheng.persistence.",
            "yuheng.knowledge.",
            "yuheng.llm.",
            "yuheng.admin.openapi.",
            "egon.cola.component.mybatis-plus.");

    /** 中文说明：运维必须提供的部署身份键，任何 profile 文档写死它们都是越权。 English summary: the deployment-identity keys the operator must supply; pinning any of them in a profile document is a breach. */
    private static final List<String> OPERATOR_ONLY_PREFIXES = List.of(
            "yuheng.persistence.", "egon.cola.component.mybatis-plus.");

    private static final Pattern FIELD = Pattern.compile("private\\s+[\\w.<>, ]+?\\s+(\\w+);");
    private static final Pattern SIXTY_FOUR_HEX = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern MDC_KEY_LITERAL = Pattern.compile("(TENANT_MDC_KEY|USER_MDC_KEY)\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");

    private static final Path ADMIN_MANIFEST = Path.of("src/main/resources/db/egon-mp/repository-manifest.json");

    @Test
    @DisplayName("三个进程绑定同一个部署身份前缀与同一组身份键")
    void allProcessesBindOneIdentityWithTheSameKeys() {
        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("tenant-id", List.of("NotNull", "Positive"));
        expected.put("identity-tenant-id", List.of("NotNull", "Positive"));
        expected.put("service-user-id", List.of("NotBlank", "Size"));
        expected.put("expected-schema-version", List.of("NotBlank", "Size"));
        expected.put("expected-schema-sha256", List.of("NotBlank", "Pattern"));
        expected.put("managed-ddl-enabled", List.of());

        Map<String, Map<String, List<String>>> observed = new LinkedHashMap<>();
        IDENTITY_BINDINGS.forEach((module, relativePath) -> observed.put(module,
                identityKeys(source(module, relativePath))));

        assertThat(observed).hasSize(IDENTITY_BINDINGS.size());
        observed.forEach((module, keys) ->
                assertThat(keys).as("identity keys declared by " + module).isEqualTo(expected));
    }

    @Test
    @DisplayName("三个进程都用严格绑定的 yuheng.persistence 前缀")
    void allProcessesUseTheSameStrictlyBoundPrefix() {
        IDENTITY_BINDINGS.forEach((module, relativePath) -> {
            java.util.regex.Matcher prefix = PREFIX.matcher(source(module, relativePath));

            assertThat(prefix.find()).as("prefix of " + module).isTrue();
            assertThat(prefix.group(1)).as("prefix of " + module).isEqualTo("yuheng.persistence");
            assertThat(source(module, relativePath)).as(module + " must reject unknown keys")
                    .contains("ignoreUnknownFields = false");
        });
    }

    @Test
    @DisplayName("部署身份在任何进程里都没有默认值")
    void noProcessDefaultsAnyDeploymentIdentityValue() {
        IDENTITY_BINDINGS.forEach((module, relativePath) -> {
            String text = source(module, relativePath);
            for (Map.Entry<String, List<String>> entry : identityKeys(text).entrySet()) {
                String declaration = fieldDeclaration(text, entry.getKey());

                assertThat(declaration).as(module + " key " + entry.getKey())
                        .doesNotContain("=")
                        .doesNotMatch("(?s).*\\b(null|0L|1L)\\b.*");
            }
        });
    }

    @Test
    @DisplayName("没有 profile 文档写死部署身份或 MDC 键，缺值只能由运维补齐")
    void noProfileDocumentCarriesTheDeploymentIdentity() throws IOException {
        List<Path> documents = profileDocuments();

        assertThat(documents).isNotEmpty();
        for (Path document : documents) {
            for (String prefix : OPERATOR_ONLY_PREFIXES) {
                assertThat(leafKeys(document, prefix))
                        .as("%s must not pin %s", document, prefix)
                        .isEmpty();
            }
        }
    }

    @Test
    @DisplayName("同一进程的基础文档与环境覆盖文档在 AI 面前缀下键结构一致")
    void baseAndOverlayKeepTheSameAiKeyStructure() throws IOException {
        int compared = 0;
        int sharedKeyFacts = 0;
        for (String module : IDENTITY_BINDINGS.keySet()) {
            Path base = Path.of("..").resolve(module).resolve("src/main/resources/application.yml");
            if (!Files.exists(base)) {
                continue;
            }
            Map<String, List<String>> baseKeys = keysByPrefix(base);
            for (Path overlay : overlays(module)) {
                Map<String, List<String>> overlayKeys = keysByPrefix(overlay);
                for (String prefix : AI_PREFIXES) {
                    assertThat(overlayKeys.getOrDefault(prefix, List.of()))
                            .as("%s overlay %s under %s", module, overlay.getFileName(), prefix)
                            .containsExactlyInAnyOrderElementsOf(baseKeys.getOrDefault(prefix, List.of()));
                    compared++;
                    sharedKeyFacts += baseKeys.getOrDefault(prefix, List.of()).size();
                }
            }
        }

        assertThat(compared).as("each declared overlay must be compared on every AI prefix").isPositive();
        assertThat(sharedKeyFacts)
                .as("at least one AI key is really declared in a bundled profile document")
                .isPositive();
    }

    @Test
    @DisplayName("受管 schema 只有一个版本与指纹事实来源，且不被任何主源码写死")
    void theManagedSchemaExpectationHasOneSourceOfTruth() throws IOException {
        JsonNode manifest = MANIFEST_MAPPER.readTree(Files.readString(ADMIN_MANIFEST, StandardCharsets.UTF_8));
        assertThat(manifest.path("family").asText()).isEqualTo("web");
        JsonNode scripts = manifest.path("scripts");
        assertThat(scripts.isArray()).isTrue();
        assertThat(scripts).hasSize(1);
        String version = scripts.get(0).path("version").asText();
        String fingerprint = scripts.get(0).path("sha256").asText();
        assertThat(version).matches("\\d{8}_\\d{3}");
        assertThat(fingerprint).matches("[0-9a-f]{64}");
        assertThat(Path.of("src/main/resources").resolve(scripts.get(0).path("path").asText()))
                .exists();

        List<String> hardCoded = new ArrayList<>();
        for (String module : IDENTITY_BINDINGS.keySet()) {
            for (Path path : mainSources(module)) {
                String code = codeOnly(Files.readString(path, StandardCharsets.UTF_8));
                if (SIXTY_FOUR_HEX.matcher(code).find() || code.contains(version)) {
                    hardCoded.add(path.toString());
                }
            }
        }
        assertThat(hardCoded).isEmpty();

        List<Path> manifestReaders = new ArrayList<>();
        for (String module : IDENTITY_BINDINGS.keySet()) {
            for (Path path : mainSources(module)) {
                if (codeOnly(Files.readString(path, StandardCharsets.UTF_8)).contains("repository-manifest")) {
                    manifestReaders.add(path);
                }
            }
        }
        assertThat(manifestReaders).hasSize(1);
        assertThat(manifestReaders.get(0).getFileName().toString()).isEqualTo("GatewayManagedDdlFactory.java");
    }

    @Test
    @DisplayName("跨进程携带租户的 MDC 键与组件默认键同源")
    void theMdcKeysMatchTheComponentDefaults() {
        String componentDefaults = new EgonColaMybatisPlusProperties().getAudit().getUserIdMdcKey();
        Map<String, String> mcpKeys = mdcKeys(source("yuheng-mcp-gateway",
                "src/main/java/top/egon/cola/component/yuheng/mcp/engine/mcp/adapter/support/"
                        + "McpGatewayPersistenceContext.java"));

        assertThat(mcpKeys).containsEntry("TENANT_MDC_KEY", EgonColaTenantIdProvider.DEFAULT_MDC_KEY);
        assertThat(mcpKeys).containsEntry("USER_MDC_KEY", componentDefaults);

        for (Map.Entry<String, String> entry : Map.of(
                "yuheng-admin", "src/main/java/top/egon/cola/component/yuheng/admin/config/"
                        + "GatewayPersistenceContextComponent.java",
                "yuheng-llm-gateway", "src/main/java/top/egon/cola/component/yuheng/llm/config/"
                        + "LlmPersistenceContextComponent.java").entrySet()) {
            String context = source(entry.getKey(), entry.getValue());

            assertThat(context).as(entry.getKey() + " must read the tenant key from the component")
                    .contains("getTenantId().getMdcKey()");
            assertThat(context).as(entry.getKey() + " must read the audit key from the component")
                    .contains("getAudit().getUserIdMdcKey()");
            assertThat(context).doesNotContain("\"tenantId\"");
            assertThat(context).doesNotContain("\"userId\"");
        }
    }

    private static final Pattern PREFIX = Pattern.compile("prefix\\s*=\\s*\"([^\"]+)\"");

    /**
     * 中文说明：把绑定类源码里的字段读成「配置键 → 约束注解」的有序映射，作为跨进程比对的事实。
     * English summary: Reads a binding class into an ordered {@code config key -> constraint annotations} map, which is
     * the fact three processes are compared on.
     * @param source 参数 绑定类源码；parameter the binding class source.
     * @return 返回 键到注解集的映射；returns the key-to-annotations mapping.
     */
    private static Map<String, List<String>> identityKeys(String source) {
        Map<String, List<String>> keys = new LinkedHashMap<>();
        List<String> pending = new ArrayList<>();
        for (String rawLine : source.split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith("public class") || line.startsWith("class ")) {
                pending.clear();
            } else if (line.startsWith("@")) {
                pending.add(line.substring(1).replaceAll("[(:].*", ""));
            } else {
                java.util.regex.Matcher field = FIELD.matcher(line);
                if (field.matches() && !line.contains(" static ")) {
                    keys.put(kebab(field.group(1)), List.copyOf(pending));
                    pending.clear();
                }
            }
        }
        return keys;
    }

    private static String fieldDeclaration(String source, String kebabKey) {
        for (String rawLine : source.split("\n")) {
            java.util.regex.Matcher field = FIELD.matcher(rawLine.trim());
            if (field.matches() && kebab(field.group(1)).equals(kebabKey)) {
                return rawLine.trim();
            }
        }
        throw new AssertionError("no field declaration for key " + kebabKey);
    }

    private static Map<String, String> mdcKeys(String source) {
        Map<String, String> keys = new LinkedHashMap<>();
        java.util.regex.Matcher matcher = MDC_KEY_LITERAL.matcher(source);
        while (matcher.find()) {
            keys.put(matcher.group(1), matcher.group(2));
        }
        return keys;
    }

    private static String kebab(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
    }

    private static String source(String module, String relativePath) {
        Path path = Path.of("..").resolve(module).resolve(relativePath);
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException("cannot read " + path, failure);
        }
    }

    private static List<Path> mainSources(String module) throws IOException {
        try (Stream<Path> paths = Files.walk(Path.of("..").resolve(module).resolve("src/main/java"))) {
            return paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
    }

    private List<Path> profileDocuments() throws IOException {
        List<Path> documents = new ArrayList<>();
        for (String module : IDENTITY_BINDINGS.keySet()) {
            documents.addAll(profileDocuments(module));
        }
        return documents;
    }

    private static List<Path> profileDocuments(String module) throws IOException {
        Path root = Path.of("..").resolve(module).resolve("src/main/resources");
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(root)) {
            return paths.filter(path -> path.getFileName().toString().startsWith("application"))
                    .filter(path -> path.getFileName().toString().endsWith(".yml"))
                    .sorted()
                    .toList();
        }
    }

    private static List<Path> overlays(String module) throws IOException {
        List<Path> overlays = new ArrayList<>(profileDocuments(module));
        overlays.remove(Path.of("..").resolve(module).resolve("src/main/resources/application.yml"));
        return overlays;
    }

    private static Map<String, List<String>> keysByPrefix(Path document) throws IOException {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (String prefix : AI_PREFIXES) {
            grouped.put(prefix, leafKeys(document, prefix));
        }
        return grouped;
    }

    private static List<String> leafKeys(Path document, String prefix) throws IOException {
        List<String> keys = new ArrayList<>();
        for (PropertySource<?> source : new YamlPropertySourceLoader()
                .load(document.getFileName().toString(), new FileSystemResource(document))) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                for (String name : enumerable.getPropertyNames()) {
                    if (name.startsWith(prefix)) {
                        keys.add(name.substring(prefix.length()));
                    }
                }
            }
        }
        return keys;
    }

    private static String codeOnly(String text) {
        return LINE_COMMENT.matcher(BLOCK_COMMENT.matcher(text).replaceAll(" ")).replaceAll(" ");
    }
}
