package top.egon.cola.component.gateway.mcp.resource;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResourceTemplate;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Resolves exact resources and reviewed URI templates from active rules.
 */
public final class McpResourceCatalog {

    private final Supplier<CompiledMcpRules> rules;

    private final McpResourceUriValidator validator;

    public McpResourceCatalog(
            Supplier<CompiledMcpRules> rules,
            McpResourceUriValidator validator) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public List<McpRuntimeResource> resources(String serverCode) {
        return active().resourcesByQualifiedName().values().stream()
                .filter(McpRuntimeResource::enabled)
                .filter(resource -> resource.serverCode().equals(serverCode))
                .filter(resource -> resource.remoteMountId() == null)
                .sorted(java.util.Comparator.comparing(
                        McpRuntimeResource::name
                ))
                .toList();
    }

    public List<McpRuntimeResourceTemplate> templates(String serverCode) {
        return active().templatesByQualifiedName().values().stream()
                .filter(McpRuntimeResourceTemplate::enabled)
                .filter(template -> template.serverCode().equals(serverCode))
                .filter(template -> template.remoteMountId() == null)
                .sorted(java.util.Comparator.comparing(
                        McpRuntimeResourceTemplate::name
                ))
                .toList();
    }

    public ResolvedResource resolve(String serverCode, String uri) {
        URI validated = validator.validate(uri);
        for (McpRuntimeResource resource : resources(serverCode)) {
            if (validator.validate(resource.uri()).equals(validated)) {
                return ResolvedResource.exact(resource, validated.toString());
            }
        }
        for (McpRuntimeResourceTemplate template : templates(serverCode)) {
            Map<String, String> variables = match(template, validated);
            if (variables != null) {
                return ResolvedResource.template(
                        template,
                        validated.toString(),
                        variables
                );
            }
        }
        throw McpResourceDriver.rejected("MCP resource was not found");
    }

    private Map<String, String> match(
            McpRuntimeResourceTemplate template,
            URI concrete) {
        McpResourceUriValidator.Template checked =
                validator.validateTemplate(template.uriTemplate());
        URI descriptor = URI.create(checked.value().replaceAll(
                "\\{[A-Za-z][A-Za-z0-9_]{0,63}}",
                "template-value"
        ));
        if (!descriptor.getRawAuthority().equals(concrete.getRawAuthority())) {
            return null;
        }
        String[] pattern = URI.create(checked.value().replace("{", "%7B")
                        .replace("}", "%7D"))
                .getRawPath()
                .split("/", -1);
        String[] values = concrete.getRawPath().split("/", -1);
        if (pattern.length != values.length) {
            return null;
        }
        LinkedHashMap<String, String> variables = new LinkedHashMap<>();
        for (int index = 0; index < pattern.length; index++) {
            String segment = pattern[index]
                    .replace("%7B", "{")
                    .replace("%7D", "}");
            if (segment.matches("\\{[A-Za-z][A-Za-z0-9_]{0,63}}")) {
                if (values[index].isBlank()) {
                    return null;
                }
                variables.put(
                        segment.substring(1, segment.length() - 1),
                        values[index]
                );
            } else if (!segment.equals(values[index])) {
                return null;
            }
        }
        return Collections.unmodifiableMap(variables);
    }

    private CompiledMcpRules active() {
        CompiledMcpRules active = rules.get();
        return active == null ? CompiledMcpRules.empty() : active;
    }

    public record ResolvedResource(
            McpRuntimeResource resource,
            McpRuntimeResourceTemplate template,
            String uri,
            Map<String, String> uriVariables
    ) {

        public ResolvedResource {
            if ((resource == null) == (template == null)) {
                throw new IllegalArgumentException(
                        "exactly one MCP resource descriptor is required"
                );
            }
            uri = Objects.requireNonNull(uri, "uri");
            uriVariables = Map.copyOf(uriVariables);
        }

        public static ResolvedResource exact(
                McpRuntimeResource resource,
                String uri) {
            return new ResolvedResource(resource, null, uri, Map.of());
        }

        public static ResolvedResource template(
                McpRuntimeResourceTemplate template,
                String uri,
                Map<String, String> variables) {
            return new ResolvedResource(null, template, uri, variables);
        }

        public String serverCode() {
            return resource == null
                    ? template.serverCode()
                    : resource.serverCode();
        }

        public String name() {
            return resource == null ? template.name() : resource.name();
        }

        public String mimeType() {
            return resource == null
                    ? template.mimeType()
                    : resource.mimeType();
        }

        public String driverType() {
            return resource == null
                    ? template.driverType()
                    : resource.driverType();
        }

        public String operationId() {
            return resource == null
                    ? template.operationId()
                    : resource.operationId();
        }

        public Map<String, String> configuration() {
            return resource == null
                    ? template.configuration()
                    : resource.configuration();
        }

        public long maximumBytes() {
            long configured = resource == null
                    ? template.maxBytes()
                    : resource.maxBytes();
            return configured == 0L ? 4L * 1024 * 1024 : configured;
        }

        public McpResourceDriver.ReadRequest request(
                Map<String, Object> attributes) {
            return new McpResourceDriver.ReadRequest(
                    serverCode(),
                    name(),
                    uri,
                    mimeType(),
                    operationId(),
                    configuration(),
                    uriVariables,
                    maximumBytes(),
                    attributes
            );
        }
    }
}
