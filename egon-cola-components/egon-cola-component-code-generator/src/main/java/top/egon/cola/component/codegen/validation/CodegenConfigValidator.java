package top.egon.cola.component.codegen.validation;

import jakarta.validation.ConstraintViolation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.codegen.model.CodegenConfigBO;
import top.egon.cola.component.codegen.model.CodegenPlanBO;
import top.egon.cola.component.codegen.model.CodegenProfileEnum;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Annotation-driven and cross-field checks for a generator request.
 *
 * <p>The tool is not a Spring bean. Callers construct it and pass {@link ValidationUtils}.
 * Validation never creates output directories and never adds artifacts that were not requested.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class CodegenConfigValidator {

    public static final String CONFIG_REQUIRED = "CONFIG_REQUIRED";

    public static final String UNSUPPORTED_ARTIFACT = "UNSUPPORTED_ARTIFACT";

    public static final String DUPLICATE_PATH = "DUPLICATE_PATH";

    public static final String MISSING_TYPE = "MISSING_TYPE";

    public static final String FIELD_POLICY_REQUIRED = "FIELD_POLICY_REQUIRED";

    public static final String SERVER_FIELD_EXPOSURE = "SERVER_FIELD_EXPOSURE";

    public static final String UNSUPPORTED_FORMAT = "UNSUPPORTED_FORMAT";

    private static final Set<String> KNOWN_ARTIFACTS = Set.of(
            "po", "dao", "mapper-xml", "repo", "converter", "command", "query", "result",
            "domain-service", "domain-impl", "domain-model", "domain-query", "manage", "manage-impl", "controller");

    private static final List<String> PERSISTENCE_ARTIFACTS = List.of("po", "dao", "mapper-xml", "repo");

    private static final List<String> BACKEND_CRUD_ARTIFACTS = List.of(
            "po", "dao", "mapper-xml", "repo", "domain-model", "domain-query", "command", "query",
            "result", "converter", "domain-service", "domain-impl", "manage", "manage-impl");

    private static final Set<String> UPPER_ARTIFACTS = Set.of(
            "converter", "command", "query", "result", "domain-service", "domain-impl",
            "domain-model", "domain-query", "manage", "manage-impl", "controller");

    private static final Set<String> SERVER_FIELDS = Set.of(
            "tenant_id", "tenantId", "created_at", "createdAt", "created_by", "createdBy",
            "updated_at", "updatedAt", "updated_by", "updatedBy", "deleted_at", "deletedAt");

    @Qualifier("validationUtils")
    private final ValidationUtils validationUtils;

    public List<CodegenPlanBO.DiagnosticBO> validate(CodegenConfigBO config) {
        if (config == null) {
            return List.of(diagnostic(CONFIG_REQUIRED, "", "config is required"));
        }
        List<CodegenPlanBO.DiagnosticBO> diagnostics = new ArrayList<>();
        for (ConstraintViolation<CodegenConfigBO> violation : validationUtils.violations(
                config, CodegenGroups.Plan.class)) {
            diagnostics.add(diagnostic(CONFIG_REQUIRED, pointer(violation.getPropertyPath().toString()),
                    violation.getMessage()));
        }
        if (config.getConfigVersion() != null && config.getConfigVersion() != 1) {
            diagnostics.add(diagnostic(CONFIG_REQUIRED, "/configVersion", "configVersion must be 1"));
        }
        diagnostics.addAll(crossChecks(config));
        if (!diagnostics.isEmpty()) {
            log.warn("codegen config rejected: {}", diagnostics.stream()
                    .map(CodegenPlanBO.DiagnosticBO::getCode)
                    .distinct()
                    .toList());
        }
        return List.copyOf(diagnostics);
    }

    public List<CodegenPlanBO.DiagnosticBO> validateApply(CodegenPlanBO plan) {
        if (plan == null) {
            return List.of(diagnostic(CONFIG_REQUIRED, "", "plan is required"));
        }
        List<CodegenPlanBO.DiagnosticBO> diagnostics = new ArrayList<>();
        for (ConstraintViolation<CodegenPlanBO> violation : validationUtils.violations(
                plan, CodegenGroups.Apply.class)) {
            diagnostics.add(diagnostic(CONFIG_REQUIRED, pointer(violation.getPropertyPath().toString()),
                    violation.getMessage()));
        }
        if (!plan.supportedFormat()) {
            diagnostics.add(diagnostic(UNSUPPORTED_FORMAT, "/formatVersion", "formatVersion must be 2"));
        }
        if (!diagnostics.isEmpty()) {
            log.warn("codegen apply rejected: {}", diagnostics.stream()
                    .map(CodegenPlanBO.DiagnosticBO::getCode)
                    .distinct()
                    .toList());
        }
        return List.copyOf(diagnostics);
    }

    /**
     * Returns a detached config with aliases and preset tokens normalized.
     * Referenced PO/DAO types are not turned into extra artifacts.
     *
     * @param config original request
     * @return normalized copy
     */
    public CodegenConfigBO normalize(CodegenConfigBO config) {
        if (config == null) {
            throw new IllegalArgumentException("config is required");
        }
        CodegenConfigBO copy = config.snapshot();
        copy.setArtifacts(new ArrayList<>(expand(config)));
        return copy;
    }

    private List<CodegenPlanBO.DiagnosticBO> crossChecks(CodegenConfigBO config) {
        List<CodegenPlanBO.DiagnosticBO> diagnostics = new ArrayList<>();
        List<String> expanded = expand(config);
        Set<String> seen = new LinkedHashSet<>();
        for (String artifact : expanded) {
            if (!seen.add(artifact)) {
                diagnostics.add(diagnostic(DUPLICATE_PATH, "/artifacts", "duplicate artifact " + artifact));
            }
        }
        if (config.getProfile() == CodegenProfileEnum.SERVICE && seen.contains("controller")) {
            diagnostics.add(diagnostic(UNSUPPORTED_ARTIFACT, "/artifacts",
                    "service profile does not generate controller"));
        }
        diagnostics.addAll(checkInput(config.getInput()));
        diagnostics.addAll(checkRoots(config));
        diagnostics.addAll(checkTables(config.getTables()));
        diagnostics.addAll(checkExistingTypes(seen, config.getExistingTypes()));
        diagnostics.addAll(checkFieldPolicies(seen, config.getFieldPolicies()));
        diagnostics.addAll(checkApiContract(config.getProfile(), seen, config.getApiContract()));
        return diagnostics;
    }

    private static List<String> expand(CodegenConfigBO config) {
        List<String> expanded = new ArrayList<>();
        List<String> artifacts = config.getArtifacts();
        if (artifacts == null) {
            return expanded;
        }
        for (String artifact : artifacts) {
            if (artifact == null || artifact.isBlank()) {
                expanded.add("");
                continue;
            }
            String token = alias(artifact.trim());
            if ("persistence-crud".equals(token)) {
                expanded.addAll(PERSISTENCE_ARTIFACTS);
            } else if ("backend-crud".equals(token)) {
                expanded.addAll(BACKEND_CRUD_ARTIFACTS);
                if (config.getProfile() != CodegenProfileEnum.SERVICE) {
                    expanded.add("controller");
                }
            } else {
                expanded.add(token);
            }
        }
        return expanded;
    }

    private static String alias(String artifact) {
        return switch (artifact) {
            case "pojo" -> "po";
            case "mapper" -> "mapper-xml";
            default -> artifact;
        };
    }

    private static List<CodegenPlanBO.DiagnosticBO> checkInput(CodegenConfigBO.InputBO input) {
        if (input == null || input.getMode() == null || input.getMode().isBlank()) {
            return List.of();
        }
        List<CodegenPlanBO.DiagnosticBO> diagnostics = new ArrayList<>();
        if ("schema".equals(input.getMode())) {
            if (input.getSchemaFiles() == null || input.getSchemaFiles().isEmpty()) {
                diagnostics.add(diagnostic(CONFIG_REQUIRED, "/input/schemaFiles",
                        "schema mode requires schemaFiles"));
            }
            if (!blank(input.getManifest())) {
                diagnostics.add(diagnostic(CONFIG_REQUIRED, "/input/manifest",
                        "schema mode does not accept a manifest"));
            }
            return diagnostics;
        }
        if ("manifest".equals(input.getMode())) {
            if (blank(input.getManifest())) {
                diagnostics.add(diagnostic(CONFIG_REQUIRED, "/input/manifest",
                        "manifest mode requires a manifest"));
            }
            if (blank(input.getResourceRoot())) {
                diagnostics.add(diagnostic(CONFIG_REQUIRED, "/input/resourceRoot",
                        "manifest mode requires resourceRoot"));
            }
            if (input.getSchemaFiles() != null && !input.getSchemaFiles().isEmpty()) {
                diagnostics.add(diagnostic(CONFIG_REQUIRED, "/input/schemaFiles",
                        "manifest mode does not accept schemaFiles"));
            }
            return diagnostics;
        }
        return List.of(diagnostic(CONFIG_REQUIRED, "/input/mode", "input mode must be schema or manifest"));
    }

    private static List<CodegenPlanBO.DiagnosticBO> checkRoots(CodegenConfigBO config) {
        if (config.getProfile() != CodegenProfileEnum.WEB && config.getProfile() != CodegenProfileEnum.SERVICE) {
            return List.of();
        }
        Map<String, String> roots = config.getRoots();
        if (roots == null || roots.isEmpty()) {
            return List.of(diagnostic(CONFIG_REQUIRED, "/modulePaths", "web and service require modulePaths"));
        }
        List<CodegenPlanBO.DiagnosticBO> diagnostics = new ArrayList<>();
        Set<String> values = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : roots.entrySet()) {
            if (blank(entry.getKey()) || blank(entry.getValue())) {
                diagnostics.add(diagnostic(CONFIG_REQUIRED, "/modulePaths", "modulePaths entries must be non-blank"));
            } else if (!values.add(entry.getValue())) {
                diagnostics.add(diagnostic(DUPLICATE_PATH, "/modulePaths",
                        "duplicate module path " + entry.getValue()));
            }
        }
        return diagnostics;
    }

    private static List<CodegenPlanBO.DiagnosticBO> checkTables(List<String> tables) {
        if (tables == null) {
            return List.of();
        }
        List<CodegenPlanBO.DiagnosticBO> diagnostics = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String table : tables) {
            if (table != null && !seen.add(table)) {
                diagnostics.add(diagnostic(DUPLICATE_PATH, "/logicalTables", "duplicate logical table " + table));
            }
        }
        return diagnostics;
    }

    private static List<CodegenPlanBO.DiagnosticBO> checkExistingTypes(
            Set<String> artifacts, Map<String, String> existingTypes) {
        List<CodegenPlanBO.DiagnosticBO> diagnostics = new ArrayList<>();
        if (needsType(artifacts, "po", "dao", "mapper-xml", "repo") && !artifacts.contains("po")
                && blank(mapping(existingTypes, "po"))) {
            diagnostics.add(missingType("po"));
        }
        if (needsType(artifacts, "dao", "mapper-xml", "repo") && !artifacts.contains("dao")
                && blank(mapping(existingTypes, "dao"))) {
            diagnostics.add(missingType("dao"));
        }
        for (String artifact : artifacts) {
            if (!artifact.isEmpty() && !KNOWN_ARTIFACTS.contains(artifact)) {
                diagnostics.add(diagnostic(UNSUPPORTED_ARTIFACT, "/artifacts", "unsupported artifact " + artifact));
            }
        }
        return diagnostics;
    }

    private static boolean needsType(Set<String> artifacts, String... triggers) {
        for (String trigger : triggers) {
            if (artifacts.contains(trigger)) {
                return true;
            }
        }
        return false;
    }

    private static CodegenPlanBO.DiagnosticBO missingType(String typeName) {
        CodegenPlanBO.DiagnosticBO diagnostic = diagnostic(MISSING_TYPE, "/existingTypeMappings/" + typeName,
                "missing existing type " + typeName);
        diagnostic.setArtifact(typeName);
        return diagnostic;
    }

    private static String mapping(Map<String, String> existingTypes, String key) {
        if (existingTypes == null) {
            return null;
        }
        return existingTypes.get(key);
    }

    private static List<CodegenPlanBO.DiagnosticBO> checkFieldPolicies(
            Set<String> artifacts, CodegenConfigBO.FieldPoliciesBO policies) {
        boolean upper = false;
        for (String artifact : artifacts) {
            if (UPPER_ARTIFACTS.contains(artifact)) {
                upper = true;
                break;
            }
        }
        if (!upper) {
            return policies == null ? List.of() : serverFieldDiagnostics(policies);
        }
        if (policies == null || policies.getCreate() == null || policies.getUpdate() == null
                || policies.getResult() == null || policies.getFilter() == null || policies.getSort() == null) {
            return List.of(diagnostic(FIELD_POLICY_REQUIRED, "/fieldPolicies",
                    "upper CRUD requires explicit create, update, result, filter and sort policies"));
        }
        return serverFieldDiagnostics(policies);
    }

    private static List<CodegenPlanBO.DiagnosticBO> serverFieldDiagnostics(CodegenConfigBO.FieldPoliciesBO policies) {
        List<CodegenPlanBO.DiagnosticBO> diagnostics = new ArrayList<>();
        rejectServerFields(diagnostics, "/fieldPolicies/create", policies.getCreate());
        rejectServerFields(diagnostics, "/fieldPolicies/update", policies.getUpdate());
        rejectServerFields(diagnostics, "/fieldPolicies/result", policies.getResult());
        return diagnostics;
    }

    private static void rejectServerFields(
            List<CodegenPlanBO.DiagnosticBO> diagnostics, String pointer, List<String> fields) {
        if (fields == null) {
            return;
        }
        for (String field : fields) {
            if (SERVER_FIELDS.contains(field)) {
                diagnostics.add(diagnostic(SERVER_FIELD_EXPOSURE, pointer,
                        "server-managed field cannot be exposed: " + field));
            }
        }
    }

    private static List<CodegenPlanBO.DiagnosticBO> checkApiContract(
            CodegenProfileEnum profile, Set<String> artifacts, CodegenConfigBO.ApiContractBO apiContract) {
        if (!artifacts.contains("controller") || profile == null || !profile.allowsController()) {
            return List.of();
        }
        if (apiContract == null || blank(apiContract.getExistingErrorMapper())
                || blank(apiContract.getContextSymbol()) || blank(apiContract.getBasePath())) {
            return List.of(diagnostic(CONFIG_REQUIRED, "/apiContract",
                    "controller requires the existing error, context and basePath symbols"));
        }
        return List.of();
    }

    private static CodegenPlanBO.DiagnosticBO diagnostic(String code, String pointer, String message) {
        return CodegenPlanBO.DiagnosticBO.of(code, pointer, message);
    }

    private static String pointer(String propertyPath) {
        if (propertyPath == null || propertyPath.isBlank()) {
            return "";
        }
        return "/" + propertyPath.replace(".", "/").replace("[", "/").replace("]", "");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
