package top.egon.cola.component.gateway.contract.rule;

import java.util.Locale;

/**
 * Form-shaped projection of a reported operation parameter.
 *
 * <p>Deliberately leaner than
 * {@code GatewayInterfaceDefinitionReport.Parameter}: it keeps only what a
 * caller needs to render and submit an interface-test request. The full JSON
 * schema and the constraint map stay on the report side, because the operation
 * already publishes {@code requestSchema} and rule content is size-bounded.
 *
 * <p>Only {@code name} and {@code location} are validated. Everything else is
 * presentation metadata and may be absent, so that a rule snapshot never
 * becomes unreadable over a missing label.
 */
public record GatewayRuntimeParameter(
        String name,
        String location,
        boolean required,
        String typeDisplay,
        String defaultValue,
        String description
) {

    public GatewayRuntimeParameter {
        name = required(name, "parameter.name");
        location = required(location, "parameter.location")
                .toUpperCase(Locale.ROOT);
        typeDisplay = optional(typeDisplay);
        defaultValue = optional(defaultValue);
        description = optional(description);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
