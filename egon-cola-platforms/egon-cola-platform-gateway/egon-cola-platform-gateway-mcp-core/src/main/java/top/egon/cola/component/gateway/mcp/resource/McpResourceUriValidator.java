package top.egon.cola.component.gateway.mcp.resource;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accepts reviewed internal and App UI namespaces and rejects traversal forms.
 */
public final class McpResourceUriValidator {

    private static final Pattern VARIABLE = Pattern.compile(
            "\\{([A-Za-z][A-Za-z0-9_]{0,63})}"
    );

    private static final Pattern AUTHORITY = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._-]{0,127}"
    );

    public URI validate(String value) {
        if (value == null || value.isBlank() || value.length() > 2048) {
            throw McpResourceDriver.rejected("MCP resource URI is invalid");
        }
        String uri = value.trim();
        String lower = uri.toLowerCase(Locale.ROOT);
        if (uri.contains("\\")
                || lower.contains("%2e")
                || lower.contains("%2f")
                || lower.contains("%5c")
                || lower.contains("%00")) {
            throw McpResourceDriver.rejected(
                    "MCP resource URI contains an unsafe encoding"
            );
        }
        URI parsed;
        try {
            parsed = URI.create(uri);
        } catch (IllegalArgumentException failure) {
            throw McpResourceDriver.rejected("MCP resource URI is invalid");
        }
        boolean supportedScheme = "egon".equalsIgnoreCase(parsed.getScheme())
                || "ui".equalsIgnoreCase(parsed.getScheme());
        if (!supportedScheme
                || parsed.getRawAuthority() == null
                || !AUTHORITY.matcher(parsed.getRawAuthority()).matches()
                || parsed.getUserInfo() != null
                || parsed.getPort() != -1
                || parsed.getRawQuery() != null
                || parsed.getRawFragment() != null) {
            throw McpResourceDriver.rejected(
                    "MCP resource URI must use an internal scheme"
            );
        }
        String path = parsed.getRawPath();
        if (path == null || !path.startsWith("/")) {
            throw McpResourceDriver.rejected("MCP resource URI path is invalid");
        }
        for (String segment : path.split("/", -1)) {
            if (".".equals(segment) || "..".equals(segment)) {
                throw McpResourceDriver.rejected(
                        "MCP resource URI traversal is forbidden"
                );
            }
        }
        return parsed;
    }

    public Template validateTemplate(String value) {
        if (value == null || value.isBlank()) {
            throw McpResourceDriver.rejected(
                    "MCP resource URI template is invalid"
            );
        }
        Matcher matcher = VARIABLE.matcher(value);
        StringBuffer concrete = new StringBuffer();
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            if (!names.add(matcher.group(1))) {
                throw McpResourceDriver.rejected(
                        "MCP resource URI template repeats a variable"
                );
            }
            matcher.appendReplacement(concrete, "template-value");
        }
        matcher.appendTail(concrete);
        if (concrete.indexOf("{") >= 0 || concrete.indexOf("}") >= 0) {
            throw McpResourceDriver.rejected(
                    "MCP resource URI template is invalid"
            );
        }
        URI checked = validate(concrete.toString());
        if (!"egon".equalsIgnoreCase(checked.getScheme())) {
            throw McpResourceDriver.rejected(
                    "MCP resource URI templates must use the egon scheme"
            );
        }
        return new Template(value.trim(), java.util.Set.copyOf(names));
    }

    public record Template(String value, java.util.Set<String> variables) {
    }
}
