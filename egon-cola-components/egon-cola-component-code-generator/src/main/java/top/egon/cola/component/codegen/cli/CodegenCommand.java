package top.egon.cola.component.codegen.cli;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.codegen.ddl.DdlSchemaService;
import top.egon.cola.component.codegen.ddl.PostgreDdlAdapter;
import top.egon.cola.component.codegen.model.CodegenConfigBO;
import top.egon.cola.component.codegen.model.CodegenPlanBO;
import top.egon.cola.component.codegen.model.CodegenSchemaBO;
import top.egon.cola.component.codegen.profile.ProjectLayoutStrategy;
import top.egon.cola.component.codegen.template.FreeMarkerTemplateService;
import top.egon.cola.component.codegen.update.GenerationApplyService;
import top.egon.cola.component.codegen.update.GenerationPlanService;
import top.egon.cola.component.codegen.update.GenerationStateRepository;
import top.egon.cola.component.codegen.validation.CodegenConfigValidator;
import top.egon.cola.component.codegen.validation.GenerationScopeValidator;
import top.egon.cola.component.codegen.validation.OutputPathValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import jakarta.validation.Validation;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

/**
 * Plain Java entry point. It does not start Spring or open a database.
 */
@Slf4j
public class CodegenCommand {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    private final CodegenConfigValidator configValidator = new CodegenConfigValidator(
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator()));
    private final DdlSchemaService schemas = new DdlSchemaService(new PostgreDdlAdapter());
    private final GenerationScopeValidator scope = new GenerationScopeValidator();
    private final FreeMarkerTemplateService templates = new FreeMarkerTemplateService();
    private final GenerationStateRepository states = new GenerationStateRepository();
    private final OutputPathValidator paths = new OutputPathValidator();
    private final GenerationPlanService plans = new GenerationPlanService(states, paths);
    private final GenerationApplyService applyService = new GenerationApplyService(states, paths);

    public static void main(String[] args) {
        int code = new CodegenCommand().run(args, System.out, System.err);
        System.exit(code);
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        try {
            if (args == null || args.length == 0) {
                return error(out, err, 2, "CONFIG_REQUIRED", "command is required");
            }
            Map<String, String> flags = new LinkedHashMap<>();
            for (int index = 1; index < args.length; index++) {
                String token = args[index];
                if (!token.startsWith("--") || index + 1 >= args.length) {
                    return error(out, err, 2, "CONFIG_REQUIRED", "unknown or incomplete option " + token);
                }
                if (flags.containsKey(token)) {
                    return error(out, err, 2, "CONFIG_REQUIRED", "duplicate option " + token);
                }
                flags.put(token, args[++index]);
            }
            return switch (args[0]) {
                case "templates" -> templates(out);
                case "plan" -> plan(flags, out, err);
                case "apply" -> apply(flags, out, err);
                case "check" -> check(flags, out, err);
                case "recover" -> recover(flags, out, err);
                default -> error(out, err, 2, "CONFIG_REQUIRED", "unknown command");
            };
        } catch (Exception exception) {
            if (exception instanceof PostgreDdlAdapter.DdlParseException failure) {
                int code = "apply".equals(args[0]) ? 6 : 2;
                return error(out, err, code, code == 6 ? "STALE_PLAN" : failure.getCode(), failure.getMessage());
            }
            log.warn("codegen command failed: {}", exception.getClass().getSimpleName());
            return error(out, err, 7, "IO_FAILURE", exception.getMessage() == null ? "command failed" : exception.getMessage());
        }
    }

    private int templates(PrintStream out) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("engine", "freemarker");
        body.put("engineVersion", "2.3.35");
        body.put("templateSetDigest", templates.getTemplateSetDigest());
        body.put("profiles", List.of("light", "web", "service"));
        List<String> artifacts = new ArrayList<>(List.of("po", "dao", "mapper-xml", "repo",
                "domain-model", "domain-query", "command", "query", "result", "converter",
                "domain-service", "domain-impl", "manage", "manage-impl", "controller"));
        artifacts.sort(String::compareTo);
        body.put("artifacts", artifacts);
        body.put("presets", List.of("persistence-crud", "backend-crud"));
        body.put("planFormatVersion", CodegenPlanBO.FORMAT_VERSION);
        out.println(MAPPER.writeValueAsString(body));
        return 0;
    }

    private int plan(Map<String, String> flags, PrintStream out, PrintStream err) throws Exception {
        Loaded loaded = load(flags, out, err);
        if (loaded.code() != 0) {
            return loaded.code();
        }
        CodegenPlanBO plan = buildPlan(loaded.config(), loaded.schema());
        Files.createDirectories(planFile(loaded.root(), plan.getPlanId()).getParent());
        MAPPER.writeValue(planFile(loaded.root(), plan.getPlanId()).toFile(), plan);
        Map<String, Object> body = summary(plan);
        out.println(MAPPER.writeValueAsString(body));
        return 0;
    }

    private int apply(Map<String, String> flags, PrintStream out, PrintStream err) throws Exception {
        if (!flags.containsKey("--plan") || !flags.containsKey("--config")) {
            return error(out, err, 2, "CONFIG_REQUIRED", "apply requires --config and --plan");
        }
        if (!flags.get("--plan").matches("[0-9a-f]{64}")) {
            return error(out, err, 2, "CONFIG_REQUIRED", "plan id must be a SHA-256 digest");
        }
        Loaded loaded = load(flags, out, err);
        if (loaded.code() != 0) {
            return loaded.code();
        }
        Path file = planFile(loaded.root(), flags.get("--plan"));
        if (!Files.exists(file)) {
            return error(out, err, 2, "CONFIG_REQUIRED", "plan was not found");
        }
        CodegenPlanBO plan = MAPPER.readValue(file.toFile(), CodegenPlanBO.class);
        if (!flags.get("--plan").equals(plan.getPlanId()) || !GenerationPlanService.hasValidId(plan)) {
            return error(out, err, 6, "STALE_PLAN", "plan id does not match");
        }
        GenerationApplyService.Result result = applyService.apply(loaded.root(), plan, Set.of(), () -> {
            try {
                Loaded fresh = load(flags, out, err);
                if (fresh.code() != 0) {
                    return null;
                }
                return new GenerationApplyService.CurrentInputs(fresh.schema().getInputFingerprint(),
                        configFingerprint(fresh.config()), templates.getTemplateSetDigest(),
                        componentFingerprint(), render(fresh.config(), fresh.schema()));
            } catch (Exception failure) {
                throw new IllegalStateException("generation inputs changed", failure);
            }
        }, -1);
        out.println(MAPPER.writeValueAsString(Map.of("status", result.status(), "detail", result.detail())));
        return switch (result.status()) {
            case "APPLIED", "NO_CHANGE" -> 0;
            case "CONFLICT" -> 4;
            case "DESTRUCTIVE_ACTION" -> 5;
            case "STALE_PLAN" -> 6;
            case "RECOVERY_REQUIRED" -> 7;
            default -> 2;
        };
    }

    private int check(Map<String, String> flags, PrintStream out, PrintStream err) throws Exception {
        Loaded loaded = load(flags, out, err);
        if (loaded.code() != 0) {
            return loaded.code();
        }
        boolean drift = false;
        for (GenerationPlanService.RenderedFile rendered : render(loaded.config(), loaded.schema())) {
            Path target = loaded.root().resolve(rendered.relativePath());
            byte[] disk = Files.exists(target) ? Files.readAllBytes(target) : null;
            if (disk == null || !GenerationPlanService.sha256(disk).equals(GenerationPlanService.sha256(rendered.bytes()))) {
                drift = true;
            }
        }
        out.println(MAPPER.writeValueAsString(Map.of("status", drift ? "DRIFT" : "CLEAN")));
        return drift ? 8 : 0;
    }

    private int recover(Map<String, String> flags, PrintStream out, PrintStream err) throws Exception {
        Loaded loaded = load(flags, out, err);
        if (loaded.code() != 0) {
            return loaded.code();
        }
        GenerationApplyService.Result result = applyService.recover(loaded.root(), "rollback");
        out.println(MAPPER.writeValueAsString(Map.of("status", result.status(), "detail", result.detail())));
        return "CONFLICT".equals(result.status()) ? 4 : 0;
    }

    private CodegenPlanBO buildPlan(CodegenConfigBO config, CodegenSchemaBO schema) {
        Path root = Path.of(config.getOutputRoot());
        return plans.plan(root, render(config, schema), config.getProfile(), schema.getInputFingerprint(),
                configFingerprint(config), schema.getVersionChecksumPrefix(), templates.getTemplateSetDigest(),
                componentFingerprint(), List.of());
    }

    private List<GenerationPlanService.RenderedFile> render(CodegenConfigBO config, CodegenSchemaBO schema) {
        ProjectLayoutStrategy layout = ProjectLayoutStrategy.resolve(config.getProfile());
        List<GenerationPlanService.RenderedFile> files = new ArrayList<>();
        for (CodegenSchemaBO.TableBO table : schema.getTables()) {
            for (String artifact : config.getArtifacts()) {
                Map<String, Object> model = scope.renderModel(config, table, artifact);
                switch (artifact) {
                    case "command" -> {
                        emit(files, config, layout, table, model, artifact, "command", "create", "createCommandType", "command");
                        emit(files, config, layout, table, model, artifact, "command", "update", "updateCommandType", "command");
                        emit(files, config, layout, table, model, artifact, "command", "delete", "deleteCommandType", "command");
                    }
                    case "query" -> {
                        emit(files, config, layout, table, model, artifact, "query", "detail", "detailQueryType", "query");
                        emit(files, config, layout, table, model, artifact, "query", "page", "pageQueryType", "query");
                    }
                    case "converter" -> {
                        emit(files, config, layout, table, model, artifact, "converter", "persistence", "persistenceConverterType", "persistence-converter");
                        emit(files, config, layout, table, model, artifact, "converter", "create", "createConverterType", "application-converter");
                        emit(files, config, layout, table, model, artifact, "converter", "update", "updateConverterType", "application-converter");
                        emit(files, config, layout, table, model, artifact, "converter", "delete", "deleteConverterType", "application-converter");
                        emit(files, config, layout, table, model, artifact, "converter", "result", "resultConverterType", "application-converter");
                    }
                    default -> {
                        String typeKey = switch (artifact) {
                            case "po" -> "poType";
                            case "dao", "mapper-xml" -> "daoType";
                            case "repo" -> "repoType";
                            case "domain-model" -> "domainType";
                            case "domain-query" -> "domainQueryType";
                            case "result" -> "resultType";
                            case "domain-service" -> "domainServiceType";
                            case "domain-impl" -> "domainImplType";
                            case "manage" -> "manageType";
                            case "manage-impl" -> "manageImplType";
                            case "controller" -> "controllerType";
                            default -> throw new IllegalArgumentException("unsupported artifact " + artifact);
                        };
                        String suffix = "mapper-xml".equals(artifact) ? ".xml" : ".java";
                        files.add(new GenerationPlanService.RenderedFile(
                                layout.relativePath(config, artifact, model.get(typeKey) + suffix),
                                artifact, table.getLogicalName(), templates.render(artifact, model)));
                    }
                }
            }
        }
        return files;
    }

    private void emit(List<GenerationPlanService.RenderedFile> files, CodegenConfigBO config,
                      ProjectLayoutStrategy layout, CodegenSchemaBO.TableBO table, Map<String, Object> model,
                      String artifact, String variantKey, String variant, String typeKey, String pathArtifact) {
        Map<String, Object> context = new LinkedHashMap<>(model);
        context.put(variantKey + "Kind", variant);
        files.add(new GenerationPlanService.RenderedFile(
                layout.relativePath(config, pathArtifact, model.get(typeKey) + ".java"),
                artifact, table.getLogicalName(), templates.render(artifact, context)));
    }

    private Loaded load(Map<String, String> flags, PrintStream out, PrintStream err) throws Exception {
        if (!flags.containsKey("--config")) {
            error(out, err, 2, "CONFIG_REQUIRED", "--config is required");
            return new Loaded(2, null, null, null);
        }
        CodegenConfigBO config = MAPPER.readValue(Path.of(flags.get("--config")).toFile(), CodegenConfigBO.class);
        List<CodegenPlanBO.DiagnosticBO> diagnostics = configValidator.validate(config);
        if (!diagnostics.isEmpty()) {
            err.println(MAPPER.writeValueAsString(Map.of("code", diagnostics.get(0).getCode(), "message", diagnostics.get(0).getMessage())));
            return new Loaded(codeFor(diagnostics.get(0).getCode()), null, null, null);
        }
        config = configValidator.normalize(config);
        Path root = Path.of(config.getOutputRoot());
        GenerationStateRepository.StateBO state = states.load(root);
        List<String> observedPrefix = new ArrayList<>();
        if (state.getVersionChecksumPrefix() != null && !state.getVersionChecksumPrefix().isBlank()) {
            for (String entry : state.getVersionChecksumPrefix().split("\\R")) {
                observedPrefix.add(entry.substring(entry.indexOf(':') + 1));
            }
        }
        CodegenSchemaBO schema = schemas.read(config, Map.of(), observedPrefix);
        if (config.getTables() == null || config.getTables().isEmpty()) {
            return new Loaded(error(out, err, 2, "CONFIG_REQUIRED", "logicalTables is required"), null, null, null);
        }
        List<CodegenSchemaBO.TableBO> selected = new ArrayList<>();
        for (String tableName : config.getTables()) {
            CodegenSchemaBO.TableBO match = schema.getTables().stream()
                    .filter(table -> tableName.equals(table.getLogicalName())).findFirst().orElse(null);
            if (match == null) {
                return new Loaded(error(out, err, 2, "CONFIG_REQUIRED", "logical table is absent: " + tableName), null, null, null);
            }
            selected.add(match);
        }
        schema.setTables(selected);
        List<CodegenPlanBO.DiagnosticBO> scopeDiagnostics = scope.validate(config, schema);
        if (!scopeDiagnostics.isEmpty()) {
            err.println(MAPPER.writeValueAsString(Map.of("code", scopeDiagnostics.get(0).getCode(), "message", scopeDiagnostics.get(0).getMessage())));
            return new Loaded(3, null, null, null);
        }
        return new Loaded(0, config, schema, root);
    }

    private static String configFingerprint(CodegenConfigBO config) {
        try {
            return GenerationPlanService.sha256(MAPPER.writeValueAsBytes(config));
        } catch (IOException exception) {
            throw new IllegalStateException("configuration fingerprint failed", exception);
        }
    }

    private static String componentFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            Path codeSource = Path.of(CodegenCommand.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            String prefix = "top/egon/cola/component/codegen/";
            if (Files.isDirectory(codeSource)) {
                Path packageRoot = codeSource.resolve(prefix);
                try (var classes = Files.walk(packageRoot)) {
                    for (Path path : classes.filter(file -> file.toString().endsWith(".class"))
                            .sorted(Comparator.comparing(path -> codeSource.relativize(path).toString())).toList()) {
                        digest.update(codeSource.relativize(path).toString().getBytes(StandardCharsets.UTF_8));
                        digest.update(Files.readAllBytes(path));
                    }
                }
            } else {
                try (ZipFile jar = new ZipFile(codeSource.toFile())) {
                    for (var entry : jar.stream().filter(file -> file.getName().startsWith(prefix)
                            && file.getName().endsWith(".class")).sorted(Comparator.comparing(java.util.zip.ZipEntry::getName)).toList()) {
                        digest.update(entry.getName().getBytes(StandardCharsets.UTF_8));
                        try (InputStream input = jar.getInputStream(entry)) {
                            digest.update(input.readAllBytes());
                        }
                    }
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException | URISyntaxException exception) {
            throw new IllegalStateException("generator fingerprint failed", exception);
        }
    }

    private static int codeFor(String code) {
        return switch (code) {
            case "MISSING_TYPE", "BASE_FIELD_TYPE", "UNKNOWN_SQL_TYPE", "UNSUPPORTED_ID" -> 3;
            case "CONFLICT" -> 4;
            default -> 2;
        };
    }

    private static Map<String, Object> summary(CodegenPlanBO plan) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("planId", plan.getPlanId());
        body.put("fileCount", plan.getFiles().size());
        body.put("pending", plan.getPendingImpacts().size());
        body.put("files", plan.getFiles().stream().map(file -> Map.of(
                "path", file.getPath(), "artifact", file.getArtifact(), "operation", file.getOperation(),
                "candidateHash", file.getCandidateHash())).toList());
        return body;
    }

    private static Path planFile(Path root, String planId) {
        return root.resolve(".egon/codegen/plans").resolve(planId + ".json");
    }

    private static int error(PrintStream out, PrintStream err, int code, String stable, String message) {
        try {
            err.println(MAPPER.writeValueAsString(Map.of("code", stable,
                    "message", message == null ? "command failed" : message)));
        } catch (IOException exception) {
            err.println("{\"code\":\"IO_FAILURE\",\"message\":\"error serialization failed\"}");
        }
        return code;
    }

    private record Loaded(int code, CodegenConfigBO config, CodegenSchemaBO schema, Path root) {
    }
}
