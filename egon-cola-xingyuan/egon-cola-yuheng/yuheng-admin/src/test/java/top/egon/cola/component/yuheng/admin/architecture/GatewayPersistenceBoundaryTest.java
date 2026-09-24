package top.egon.cola.component.yuheng.admin.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 中文说明：{@code GatewayPersistenceBoundaryTest} 固定 Step 4 的持久化边界合同：被迁移的旧持久化载体必须彻底消失，
 * 业务载体必须是符合 POJO 规范的 class，service/controller 层不得再引用任何被迁移的持久化类型，
 * 而 Jdbc 实现必须落位在声明的公开端口之后。
 * English summary: {@code GatewayPersistenceBoundaryTest} pins the Step 4 persistence-boundary contract: the migrated
 * persistence carriers disappear, the business carriers are POJO-compliant classes, the service and controller layers
 * stop referencing migrated persistence types, and the Jdbc implementations sit behind their declared public ports.
 *
 * 用法 / Usage: 作为源码级架构门禁运行；/ Run as a source-level architecture gate; it reads the module sources only and
 * never opens a database, Spring context or network connection.
 */
class GatewayPersistenceBoundaryTest {

    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern TEXT_BLOCK = Pattern.compile("//[^\\n]*");


    private static final Path MAIN = Path.of("src/main/java/top/egon/cola/component/yuheng/admin");
    private static final String BO = "domain/bo/";

    /** 本Step迁移的旧持久化载体 / the persistence carriers migrated by this Step. */
    private static final List<String> MIGRATED = List.of(
            "McpCapabilityDraftPO", "McpCapabilityRecordPO", "McpRemoteProviderDraftPO",
            "McpRemoteCapabilityPO", "McpRemoteToolDraftPO", "McpRemoteMountDraftPO",
            "McpManagedToolOverridePO", "McpApprovalPO", "McpArtifactMetadataPO", "McpServerPO",
            "McpTaskPO", "GatewayPolicyDraftPO", "GatewayRouteDraftPO", "GatewayDraftPO",
            "GatewayApplicationPO", "GatewayOpenApiSyncPO", "GatewayOpenApiSnapshotPO",
            "GatewayConsumeFailurePO", "GatewayAuditLogPO", "IdempotencyPO",
            "GatewayChunkCleanupCandidatePO", "GatewayRecoverableReleaseAttemptPO",
            "GatewayReleaseTargetPO", "GatewayReleasePublicationPO", "GatewayReleasePO",
            "GatewayReleaseAttemptPO", "GatewayOperationPO", "GatewayOperationDefinitionPO",
            "GatewayGroupPO", "GatewayStoredReportPO", "GatewayCredentialPO");

    /** 本Step声明的公开业务端口与其受守卫 MP 实现 / the declared public ports and their guarded MyBatis-Plus implementations. */
    private static final List<String> PORTS = List.of(
            "McpCapabilityDraftRepository", "McpApprovalRepository", "McpRemoteToolDraftRepository",
            "McpManagedToolOverrideRepository", "McpTaskRepository", "McpArtifactMetadataRepository",
            "McpRemoteProviderRepository", "GatewayObservabilityRepository", "GatewayAuditLogRepository",
            "IdempotencyRepository");

    @Test
    @DisplayName("migrated persistence carriers are gone from every main source")
    void migratedCarriersAreRetired() throws IOException {
        for (String legacy : MIGRATED) {
            assertFalse(Files.exists(MAIN.resolve(BO + legacy + ".java")),
                    legacy + " must not be re-created as a business carrier");
        }
        for (Path path : mainSources()) {
            String text = read(path);
            for (String legacy : MIGRATED) {
                assertFalse(Pattern.compile("\\b" + legacy + "\\b").matcher(text).find(),
                        path + " still references the retired carrier " + legacy);
            }
        }
    }

    @Test
    @DisplayName("business carriers are mutable POJO classes without persistence inheritance")
    void businessCarriersFollowThePojoContract() throws IOException {
        for (String legacy : MIGRATED) {
            String carrier = legacy.substring(0, legacy.length() - 2) + "BO";
            List<Path> found = mainSources().stream()
                    .filter(p -> p.getFileName().toString().equals(carrier + ".java"))
                    .toList();
            assertFalse(found.isEmpty(), "missing business carrier " + carrier);
            assertTrue(found.size() == 1, carrier + " must be declared exactly once: " + found);
            String text = read(found.get(0));
            assertTrue(text.contains("public class " + carrier + " "),
                    carrier + " must be an ordinary class, not a record");
            assertFalse(Pattern.compile("\\brecord\\s+" + carrier + "\\b").matcher(text).find(),
                    carrier + " must not use the record form");
            for (String annotation : List.of("@Data", "@NoArgsConstructor", "@AllArgsConstructor",
                    "@Builder", "@Accessors(chain = true)")) {
                assertTrue(text.contains(annotation), carrier + " is missing " + annotation);
            }
            assertTrue(text.contains("import lombok.experimental.Accessors;"),
                    carrier + " must import the experimental Accessors annotation");
            String code = codeOnly(text);
            for (String forbidden : List.of("jakarta.persistence", "javax.persistence",
                    "org.springframework.data", "extends EgonModel", "java.util.Date",
                    "@TableName")) {
                assertFalse(code.contains(forbidden),
                        carrier + " must stay free of " + forbidden);
            }
        }
    }

    @Test
    @DisplayName("service and controller layers only speak BO/DTO/VO, never migrated PO generics")
    void applicationLayersStayAboveThePersistenceBoundary() throws IOException {
        List<Path> above = mainSources().stream()
                .filter(p -> {
                    String rel = MAIN.relativize(p).toString().replace('\\', '/');
                    return rel.contains("/service/") || rel.contains("/controller/");
                })
                .toList();
        assertFalse(above.isEmpty(), "no service/controller sources found under " + MAIN);
        for (Path path : above) {
            String text = read(path);
            for (String legacy : MIGRATED) {
                assertFalse(Pattern.compile("\\b" + legacy + "\\b").matcher(text).find(),
                        path + " leaks the persistence carrier " + legacy);
            }
            assertFalse(Pattern.compile("\\b\\w+RecordPO\\b").matcher(text).find(),
                    path + " leaks a MyBatis-Plus row model");
        }
    }

    @Test
    @DisplayName("declared ports are interfaces and the guarded MP implementations sit behind them")
    void legacyImplementationsAreIsolatedBehindPorts() throws IOException {
        for (String port : PORTS) {
            List<Path> declared = mainSources().stream()
                    .filter(p -> p.getFileName().toString().equals(port + ".java"))
                    .toList();
            assertTrue(declared.size() == 1, port + " must exist exactly once: " + declared);
            String text = read(declared.get(0));
            assertTrue(text.contains("public interface " + port),
                    port + " must be a public interface");
            assertTrue(text.contains("@Validated"), port + " must be validated at the boundary");
            assertFalse(text.contains("lombok"), port + " must not carry Lombok DI annotations");
            Path implementation = declared.get(0).getParent().resolve("impl/Mp" + port + ".java");
            assertTrue(Files.exists(implementation),
                    port + " needs the guarded MP implementation " + implementation);
            assertTrue(read(implementation).contains("implements " + port),
                    implementation.getFileName() + " must implement " + port);
            Path superseded = declared.get(0).getParent().resolve("jdbc/Jdbc" + port + ".java");
            assertFalse(Files.exists(superseded),
                    port + " must not keep a hand-written JDBC implementation: " + superseded);
        }
    }

    private static List<Path> mainSources() throws IOException {
        try (var paths = Files.walk(MAIN)) {
            return paths.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /**
     * 中文说明：剥离块注释与行注释，使架构断言只检查代码本身。
     * English summary: Strips block and line comments so architecture assertions inspect code only.
     * @param text 参数 源文件全文；parameter raw source text。
     * @return 返回仅含代码的文本；returns the code-only text.
     */
    private static String codeOnly(String text) {
        return TEXT_BLOCK.matcher(BLOCK_COMMENT.matcher(text).replaceAll(" ")).replaceAll(" ");
    }
}
