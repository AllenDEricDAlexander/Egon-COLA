package top.egon.cola.component.yuheng.admin.architecture;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Fixes the Step 3 persistence boundary: every migrated table has one MyBatis-Plus
 * row model on the starter base type, one guarded mapper XML, and no bypass of the
 * starter's tenant/soft-delete guards.
 */
class AiMpRepositoryContractTest {

    /** The 49 managed tables of the migrated admin persistence, per the Plan. */
    private static final List<String> MP_ROW_MODELS = List.of(
            "top.egon.cola.component.yuheng.admin.application.domain.po.GatewayApplicationRecordPO",
            "top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayBusinessDomainPO",
            "top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayEntityDomainPO",
            "top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayInterfaceGroupPO",
            "top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayOperationDefinitionRecordPO",
            "top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayOperationRecordPO",
            "top.egon.cola.component.yuheng.admin.credential.domain.po.GatewayCredentialRecordPO",
            "top.egon.cola.component.yuheng.admin.group.domain.po.GatewayGroupRecordPO",
            "top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeBasePO",
            "top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeChunkPO",
            "top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeDocumentPO",
            "top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeDocumentRevisionPO",
            "top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeJobPO",
            "top.egon.cola.component.yuheng.admin.llm.domain.po.LlmChannelPO",
            "top.egon.cola.component.yuheng.admin.llm.domain.po.LlmModelPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpAppBindingDraftPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpApprovalRecordPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpArtifactMetadataRecordPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpManagedToolOverrideRecordPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpPromptDraftPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpRemoteCapabilityRecordPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpRemoteMountDraftRecordPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpRemoteProviderDraftRecordPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpRemoteToolDraftRecordPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpResourceDraftPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpResourceTemplateDraftPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpServerRecordPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpTaskPolicyDraftPO",
            "top.egon.cola.component.yuheng.admin.mcp.domain.po.McpTaskRecordPO",
            "top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayAuditLogRecordPO",
            "top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayCallEventSummaryPO",
            "top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayCallMetricMinutePO",
            "top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayConsumeFailureRecordPO",
            "top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSnapshotRecordPO",
            "top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSyncRecordPO",
            "top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseAttemptRecordPO",
            "top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseContentPO",
            "top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleasePublicationRecordPO",
            "top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseRecordPO",
            "top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetRecordPO",
            "top.egon.cola.component.yuheng.admin.reporting.domain.po.GatewayDefinitionSetOperationPO",
            "top.egon.cola.component.yuheng.admin.reporting.domain.po.GatewayDefinitionSetPO",
            "top.egon.cola.component.yuheng.admin.reporting.domain.po.GatewayHmacNoncePO",
            "top.egon.cola.component.yuheng.admin.routing.domain.po.GatewayDraftRecordPO",
            "top.egon.cola.component.yuheng.admin.routing.domain.po.GatewayPolicyDraftRecordPO",
            "top.egon.cola.component.yuheng.admin.routing.domain.po.GatewayRouteDraftRecordPO",
            "top.egon.cola.component.yuheng.admin.shared.domain.po.IdempotencyRecordPO",
            "top.egon.cola.component.yuheng.admin.wiki.domain.po.WikiPagePO",
            "top.egon.cola.component.yuheng.admin.wiki.domain.po.WikiRevisionPO"
    );

    private static final Path MAPPER_XML_ROOT =
            Path.of("src/main/resources/mybatis/mapper");

    private static final Pattern NAMESPACE = Pattern.compile(
            "<mapper\\s+namespace\\s*=\\s*\"([^\"]+)\"");

    private static final Pattern MUTATING_STATEMENT = Pattern.compile(
            "<(update|delete)\\s+id\\s*=\\s*\"([^\"]+)\"");

    @Test
    void everyMigratedRowModelExtendsTheStarterModelAndMapsOneTable() {
        assertEquals(49, MP_ROW_MODELS.size(),
                "the migrated persistence covers 49 tables");
        List<String> missing = new ArrayList<>();
        List<String> violations = new ArrayList<>();
        Map<String, String> tableOwners = new LinkedHashMap<>();
        for (String rowModel : MP_ROW_MODELS) {
            Class<?> type;
            try {
                type = Class.forName(rowModel);
            } catch (ClassNotFoundException absent) {
                missing.add(rowModel);
                continue;
            }
            if (!EgonModel.class.isAssignableFrom(getSuperclass(type, violations, rowModel))) {
                violations.add(rowModel + " does not extend the starter EgonModel");
            }
            TableName tableName = type.getDeclaredAnnotation(TableName.class);
            if (tableName == null) {
                violations.add(rowModel + " declares no @TableName");
                continue;
            }
            String table = tableName.value();
            if (!table.matches("^[a-z][a-z0-9_]*$")) {
                violations.add(rowModel + " maps a non snake_case table " + table);
            }
            String previous = tableOwners.put(table, rowModel);
            if (previous != null) {
                violations.add(table + " is mapped twice: " + previous + " and " + rowModel);
            }
            if (type.isRecord()) {
                violations.add(rowModel + " must be an ordinary class, not a record");
            }
            String source = readRowModelSource(type);
            if (source.contains("jakarta.persistence") || source.contains("javax.persistence")) {
                violations.add(rowModel + " still carries JPA mapping annotations");
            }
            if (source.contains("java.util.Date") || source.contains("java.util.Calendar")) {
                violations.add(rowModel + " must use java.time, not java.util date types");
            }
            if (!source.contains("@SuperBuilder") && !source.contains("@Builder")) {
                violations.add(rowModel + " declares no builder");
            }
            if (!source.contains("@EqualsAndHashCode(callSuper = true)")) {
                violations.add(rowModel + " must compare on the inherited base state");
            }
            if (source.replaceAll("(?s)/\\*.*?\\*/", "")
                    .replaceAll("(?m)//.*$", "")
                    .lines().noneMatch(line -> line.startsWith("    private "))) {
                violations.add(rowModel + " declares no own persisted column");
            }
        }
        assertTrue(missing.isEmpty(),
                "MP row models are missing (Step 3 not implemented): " + missing);
        assertTrue(violations.isEmpty(), "Row model boundary violations: " + violations);
        assertEquals(49, tableOwners.size(), "each table is owned by exactly one row model");
    }

    @Test
    void everyGuardedMapperXmlResolvesToItsAccessInterfaceAndFiltersActiveRows()
            throws IOException {
        List<Path> xmlFiles = mapperXmlFiles();
        assertEquals(49, xmlFiles.size(),
                "one guarded mapper XML per migrated table, found: " + xmlFiles.size());
        List<String> violations = new ArrayList<>();
        for (Path xml : xmlFiles) {
            String text = Files.readString(xml);
            Matcher namespace = NAMESPACE.matcher(text);
            if (!namespace.find()) {
                violations.add(xml + " has no <mapper namespace>");
                continue;
            }
            String interfaceName = namespace.group(1);
            if (!Files.isRegularFile(sourceOf(interfaceName))) {
                violations.add(interfaceName + " has no interface source for " + xml);
            }
            for (String statement : statements(text, "select")) {
                if (!statement.contains("deleted_at IS NULL")) {
                    violations.add(xml + " has a select without the active "
                            + "deleted_at predicate: " + statement.strip());
                }
            }
            for (String statement : statements(text, "update", "delete", "insert")) {
                if (!statement.toLowerCase().contains("tenant_id")) {
                    violations.add(xml + " has a write statement without the "
                            + "tenant column: " + statement.strip());
                }
            }
            for (String handler : typeHandlers(text)) {
                if (!Files.isRegularFile(sourceOf(handler))) {
                    violations.add(xml + " binds a missing type handler " + handler);
                }
            }
        }
        assertTrue(violations.isEmpty(), "Mapper XML boundary violations: " + violations);
    }

    @Test
    void guardedDeleteStatementsReclaimSoftDeleteAndMutatingMethodsReportRowCounts()
            throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path xml : mapperXmlFiles()) {
            String text = Files.readString(xml);
            Matcher namespace = NAMESPACE.matcher(text);
            if (!namespace.find()) {
                continue;
            }
            if (text.contains("<delete") && !text.contains("SET deleted_at")
                    && !text.contains("deleted_at =")) {
                violations.add(xml + " hard-deletes instead of the guarded soft delete");
            }
            Class<?> accessInterface;
            try {
                accessInterface = Class.forName(namespace.group(1));
            } catch (ClassNotFoundException absent) {
                violations.add(namespace.group(1) + " is not loadable for " + xml);
                continue;
            }
            Map<String, Method> methods = new LinkedHashMap<>();
            for (Method method : accessInterface.getMethods()) {
                methods.put(method.getName(), method);
            }
            Matcher statement = MUTATING_STATEMENT.matcher(text);
            while (statement.find()) {
                Method method = methods.get(statement.group(2));
                if (method == null) {
                    violations.add(namespace.group(1) + "#" + statement.group(2)
                            + " is declared in XML but not on the interface");
                } else if (method.getReturnType() == void.class) {
                    violations.add(namespace.group(1) + "#" + statement.group(2)
                            + " must return the affected row count, not void");
                }
            }
        }
        assertTrue(violations.isEmpty(), "Guarded statement violations: " + violations);
    }

    @Test
    void migratedPersistenceNeverBypassesTheStarterGuards() throws IOException {
        List<String> bypasses = new ArrayList<>();
        List<String> jpaLeftovers = new ArrayList<>();
        for (String rowModel : MP_ROW_MODELS) {
            Path source = sourceOf(rowModel);
            if (!Files.isRegularFile(source)) {
                continue;
            }
            String text = Files.readString(source);
            if (text.contains("jakarta.persistence") || text.contains("javax.persistence")) {
                jpaLeftovers.add(source.toString());
            }
        }
        try (Stream<Path> paths = Files.walk(Path.of("src/main/java"))) {
            for (Path path : paths.filter(candidate -> candidate.toString()
                    .endsWith(".java")).toList()) {
                String text = Files.readString(path);
                for (String marker : List.of("new QueryWrapper", "new LambdaQueryWrapper",
                        "new UpdateWrapper", "new LambdaUpdateWrapper",
                        "com.baomidou.mybatisplus.extension.activerecord")) {
                    if (text.contains(marker)) {
                        bypasses.add(path + " uses " + marker);
                    }
                }
            }
        }
        assertTrue(jpaLeftovers.isEmpty(),
                "MP row models must not keep JPA annotations: " + jpaLeftovers);
        assertTrue(bypasses.isEmpty(),
                "Guarded access may not be bypassed by a wrapper: " + bypasses);
    }

    private static Class<?> getSuperclass(Class<?> type, List<String> violations,
            String rowModel) {
        Class<?> superclass = type.getSuperclass();
        assertNotNull(superclass, rowModel + " has no super type");
        assertFalse(Object.class.equals(superclass),
                rowModel + " must extend the starter model");
        return superclass;
    }

    private static String readRowModelSource(Class<?> type) {
        Path source = sourceOf(type.getName());
        try {
            if (!Files.isRegularFile(source)) {
                return "";
            }
            return Files.readString(source);
        } catch (IOException failure) {
            return fail("Cannot read row model source " + source, failure);
        }
    }

    private static Path sourceOf(String fullyQualified) {
        return Path.of("src/main/java").resolve(fullyQualified.replace('.', '/')
                + ".java");
    }

    private static List<String> statements(String text, String... tags) {
        List<String> bodies = new ArrayList<>();
        for (String tag : tags) {
            Matcher matcher = Pattern.compile("<" + tag + "\\b[^>]*>(.*?)</" + tag + ">",
                    Pattern.DOTALL).matcher(text);
            while (matcher.find()) {
                bodies.add(matcher.group(1));
            }
        }
        return bodies;
    }

    private static List<String> typeHandlers(String text) {
        List<String> handlers = new ArrayList<>();
        Matcher matcher = Pattern.compile(
                "typeHandler\\s*=\\s*\"([^\"]+)\"").matcher(text);
        while (matcher.find()) {
            handlers.add(matcher.group(1));
        }
        return handlers;
    }

    private static List<Path> mapperXmlFiles() throws IOException {
        if (!Files.isDirectory(MAPPER_XML_ROOT)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(MAPPER_XML_ROOT)) {
            return paths.filter(path -> path.toString().endsWith(".xml"))
                    .sorted()
                    .toList();
        }
    }

    static {
        // Static-initialisation guard: the Plan's table set is the contract, so
        // a duplicate entry here must break before any assertion runs.
        assertTrue(MP_ROW_MODELS.stream().map(name -> name.substring(
                        name.lastIndexOf('.') + 1)).distinct().count() == 49,
                "the declared row models must be 49 distinct types");
        assertTrue(MP_ROW_MODELS.stream().allMatch(name -> name.endsWith("PO")),
                "row models keep the PO suffix");
    }
}
