package top.egon.cola.component.gateway.mcp.prompt;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Performs one-pass literal interpolation without expression evaluation.
 */
public final class StrictPromptTemplate {

    private static final int MAX_TEMPLATE_LENGTH = 256 * 1024;

    private static final int MAX_ARGUMENT_LENGTH = 32 * 1024;

    public String render(
            String template,
            List<String> declaredArguments,
            Map<String, String> arguments) {
        Objects.requireNonNull(template, "template");
        if (template.length() > MAX_TEMPLATE_LENGTH) {
            throw McpPromptDriver.invalid("MCP prompt template is too large");
        }
        Set<String> declared = declared(declaredArguments);
        Map<String, String> supplied = Map.copyOf(Objects.requireNonNull(
                arguments,
                "arguments"
        ));
        if (!declared.containsAll(supplied.keySet())) {
            throw McpPromptDriver.invalid(
                    "MCP prompt contains an undeclared argument"
            );
        }
        StringBuilder output = new StringBuilder(template.length());
        int cursor = 0;
        while (cursor < template.length()) {
            int start = template.indexOf("${", cursor);
            if (start < 0) {
                output.append(template, cursor, template.length());
                break;
            }
            output.append(template, cursor, start);
            int end = template.indexOf('}', start + 2);
            if (end < 0) {
                throw McpPromptDriver.invalid(
                        "MCP prompt expression is invalid"
                );
            }
            String name = template.substring(start + 2, end);
            if (!identifier(name) || !declared.contains(name)) {
                throw McpPromptDriver.invalid(
                        "MCP prompt expression is not declared"
                );
            }
            String value = supplied.get(name);
            if (value == null) {
                throw McpPromptDriver.invalid(
                        "MCP prompt argument is required: " + name
                );
            }
            if (value.length() > MAX_ARGUMENT_LENGTH) {
                throw McpPromptDriver.invalid(
                        "MCP prompt argument is too large: " + name
                );
            }
            output.append(value);
            cursor = end + 1;
        }
        if (output.length() > 512 * 1024) {
            throw McpPromptDriver.invalid("MCP prompt result is too large");
        }
        return output.toString();
    }

    private Set<String> declared(List<String> source) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Objects.requireNonNull(source, "declaredArguments").forEach(name -> {
            if (!identifier(name) || !result.add(name)) {
                throw McpPromptDriver.invalid(
                        "MCP prompt argument declaration is invalid"
                );
            }
        });
        return Set.copyOf(result);
    }

    private static boolean identifier(String value) {
        return value != null
                && value.matches("[A-Za-z][A-Za-z0-9_]{0,63}");
    }
}
