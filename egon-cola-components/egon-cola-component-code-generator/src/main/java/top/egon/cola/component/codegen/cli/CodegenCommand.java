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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        out.println(MAPPER.writeValueAsString(body));
        return 0;
    }

    private int plan(Map<String, String> flags, PrintStream out, PrintStream err) throws Exception {
        Loaded loaded = load(flags, err);
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
        Loaded loaded = load(flags, err);
        if (loaded.code() != 0) {
            return loaded.code();
        }
        Path file = planFile(loaded.root(), flags.get("--plan"));
        if (!Files.exists(file)) {
            return error(out, err, 2, "CONFIG_REQUIRED", "plan was not found");
        }
        CodegenPlanBO plan = MAPPER.readValue(file.toFile(), CodegenPlanBO.class);
        if (!flags.get("--plan").equals(plan.getPlanId())) {
            return error(out, err, 6, "STALE_PLAN", "plan id does not match");
        }
        GenerationApplyService.Result result = applyService.apply(loaded.root(), plan, Set.of(),
                plan.getTemplateSetVersion(), plan.getComponentFingerprint(), -1);
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
        Loaded loaded = load(flags, err);
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
        Loaded loaded = load(flags, err);
        if (loaded.code() != 0) {
            return loaded.code();
        }
        GenerationApplyService.Result result = applyService.recover(loaded.root(), "rollback");
        out.println(MAPPER.writeValueAsString(Map.of("status", result.status(), "detail", result.detail())));
        return "CONFLICT".equals(result.status()) ? 4 : 0;
    }

    private CodegenPlanBO buildPlan(CodegenConfigBO config, CodegenSchemaBO schema) {
        Path root = Path.of(config.getOutputRoot());
        return plans.plan(root, render(config, schema), schema.getInputFingerprint(),
                templates.getTemplateSetDigest(), "egon-cola-component-code-generator", List.of());
    }

    private List<GenerationPlanService.RenderedFile> render(CodegenConfigBO config, CodegenSchemaBO schema) {
        ProjectLayoutStrategy layout = ProjectLayoutStrategy.resolve(config.getProfile());
        List<GenerationPlanService.RenderedFile> files = new ArrayList<>();
        for (CodegenSchemaBO.TableBO table : schema.getTables()) {
            for (String artifact : config.getArtifacts()) {
                if (!ProjectLayoutStrategy.PERSISTENCE.contains(artifact)) {
                    continue;
                }
                Map<String, Object> model = scope.renderModel(config, table, artifact);
                String fileName = switch (artifact) {
                    case "po" -> model.get("poType") + ".java";
                    case "dao" -> model.get("daoType") + ".java";
                    case "mapper-xml" -> model.get("daoType") + ".xml";
                    case "repo" -> model.get("repoType") + ".java";
                    default -> artifact;
                };
                String relative = layout.relativePath(config, artifact, fileName);
                files.add(new GenerationPlanService.RenderedFile(relative, artifact, table.getLogicalName(),
                        templates.render(artifact, model)));
            }
        }
        return files;
    }

    private Loaded load(Map<String, String> flags, PrintStream err) throws Exception {
        if (!flags.containsKey("--config")) {
            error(System.out, err, 2, "CONFIG_REQUIRED", "--config is required");
            return new Loaded(2, null, null, null);
        }
        CodegenConfigBO config = MAPPER.readValue(Path.of(flags.get("--config")).toFile(), CodegenConfigBO.class);
        List<CodegenPlanBO.DiagnosticBO> diagnostics = configValidator.validate(config);
        if (!diagnostics.isEmpty()) {
            err.println(MAPPER.writeValueAsString(Map.of("code", diagnostics.get(0).getCode(), "message", diagnostics.get(0).getMessage())));
            return new Loaded(codeFor(diagnostics.get(0).getCode()), null, null, null);
        }
        CodegenSchemaBO schema = schemas.read(config);
        List<CodegenPlanBO.DiagnosticBO> scopeDiagnostics = scope.validate(config, schema);
        if (!scopeDiagnostics.isEmpty()) {
            err.println(MAPPER.writeValueAsString(Map.of("code", scopeDiagnostics.get(0).getCode(), "message", scopeDiagnostics.get(0).getMessage())));
            return new Loaded(3, null, null, null);
        }
        return new Loaded(0, config, schema, Path.of(config.getOutputRoot()));
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
        return body;
    }

    private static Path planFile(Path root, String planId) {
        return root.resolve(".egon/codegen/plans").resolve(planId + ".json");
    }

    private static int error(PrintStream out, PrintStream err, int code, String stable, String message) {
        err.println("{\"code\":\"" + stable + "\",\"message\":\"" + message.replace("\"", "'") + "\"}");
        return code;
    }

    private record Loaded(int code, CodegenConfigBO config, CodegenSchemaBO schema, Path root) {
    }
}
