package top.egon.cola.component.codegen.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.core.PlainTextOutputFormat;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controlled FreeMarker 2.3.35 renderer. Templates stay on the classpath and failures produce no output.
 */
@Slf4j
public class FreeMarkerTemplateService {

    public static final String UNKNOWN_TEMPLATE = "UNKNOWN_TEMPLATE";

    public static final String TEMPLATE_ERROR = "TEMPLATE_ERROR";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Configuration configuration;

    private final Map<String, CatalogEntry> catalog;

    @Getter
    private final String templateSetDigest;

    public FreeMarkerTemplateService() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_35);
        cfg.setClassLoaderForTemplateLoading(FreeMarkerTemplateService.class.getClassLoader(), "templates/backend");
        cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setAPIBuiltinEnabled(false);
        cfg.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        cfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        cfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        cfg.setOutputFormat(PlainTextOutputFormat.INSTANCE);
        cfg.setLocale(Locale.ROOT);
        cfg.setClassicCompatible(false);
        this.configuration = cfg;
        this.catalog = loadCatalog();
        this.templateSetDigest = digestCatalog();
    }

    public byte[] render(String templateId, Map<String, Object> model) {
        CatalogEntry entry = catalog.get(templateId);
        if (entry == null) {
            log.warn("unknown template {}", templateId);
            throw new TemplateRenderException(UNKNOWN_TEMPLATE, templateId, null, "template is not in the catalog");
        }
        if (model == null) {
            throw new TemplateRenderException(TEMPLATE_ERROR, templateId, null, "render model is required");
        }
        checkModel(templateId, model);
        try {
            Template template = configuration.getTemplate(entry.resource());
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString().replace("\r\n", "\n").replace('\r', '\n').getBytes(StandardCharsets.UTF_8);
        } catch (TemplateException | IOException exception) {
            log.warn("template {} failed: {}", templateId, exception.getClass().getSimpleName());
            throw new TemplateRenderException(TEMPLATE_ERROR, templateId, line(exception), "template rendering failed");
        }
    }

    public boolean hasTemplate(String templateId) {
        return catalog.containsKey(templateId);
    }

    private Map<String, CatalogEntry> loadCatalog() {
        try {
            JsonNode root = MAPPER.readTree(readBytes("templates/backend/catalog.json"));
            if (root.path("formatVersion").asInt() != 1
                    || !"freemarker".equals(root.path("engine").asText())
                    || !"2.3.35".equals(root.path("engineVersion").asText())) {
                throw new TemplateRenderException(TEMPLATE_ERROR, "catalog", null, "catalog engine contract is invalid");
            }
            Map<String, CatalogEntry> entries = new LinkedHashMap<>();
            for (JsonNode node : root.path("entries")) {
                String resource = node.path("templateResource").asText();
                if (FreeMarkerTemplateService.class.getClassLoader().getResource("templates/backend/" + resource) == null) {
                    throw new TemplateRenderException(TEMPLATE_ERROR, node.path("id").asText(), null,
                            "catalog resource is missing");
                }
                entries.put(node.path("id").asText(), new CatalogEntry(
                        node.path("id").asText(),
                        node.path("artifact").asText(),
                        resource,
                        textList(node.path("allowedProfiles")),
                        node.path("targetPathPattern").asText()));
            }
            return Map.copyOf(entries);
        } catch (IOException exception) {
            throw new TemplateRenderException(TEMPLATE_ERROR, "catalog", null, "catalog could not be read");
        }
    }

    private String digestCatalog() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(readBytes("templates/backend/catalog.json"));
            for (CatalogEntry entry : catalog.values()) {
                digest.update(entry.id().getBytes(StandardCharsets.UTF_8));
                digest.update(readBytes("templates/backend/" + entry.resource()));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException exception) {
            throw new IllegalStateException("template catalog digest failed", exception);
        }
    }

    private static byte[] readBytes(String resource) throws IOException {
        try (var input = FreeMarkerTemplateService.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException(resource);
            }
            return input.readAllBytes();
        }
    }

    private static List<String> textList(JsonNode node) {
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return List.copyOf(values);
    }

    private static void checkModel(String templateId, Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof java.time.temporal.Temporal) {
            return;
        }
        if (value instanceof List<?> list) {
            list.forEach(item -> checkModel(templateId, item));
            return;
        }
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, item) -> {
                if (!(key instanceof String)) {
                    throw new TemplateRenderException(TEMPLATE_ERROR, templateId, null, "model keys must be strings");
                }
                checkModel(templateId, item);
            });
            return;
        }
        throw new TemplateRenderException(TEMPLATE_ERROR, templateId, null, "model value is not a scalar or list");
    }

    private static Integer line(Exception exception) {
        if (exception instanceof TemplateException templateException && templateException.getLineNumber() != null) {
            return templateException.getLineNumber();
        }
        return null;
    }

    public record CatalogEntry(String id, String artifact, String resource, List<String> allowedProfiles,
                               String targetPathPattern) {
    }

    public static final class TemplateRenderException extends RuntimeException {

        private final String code;

        private final String templateId;

        private final Integer line;

        public TemplateRenderException(String code, String templateId, Integer line, String message) {
            super(message);
            this.code = code;
            this.templateId = templateId;
            this.line = line;
        }

        public String getCode() {
            return code;
        }

        public String getTemplateId() {
            return templateId;
        }

        public Integer getLine() {
            return line;
        }
    }
}
