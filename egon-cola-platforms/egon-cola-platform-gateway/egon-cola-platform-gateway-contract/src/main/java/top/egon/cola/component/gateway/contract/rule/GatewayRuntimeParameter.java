package top.egon.cola.component.gateway.contract.rule;

import java.util.Locale;

/**
 * Form-shaped projection of a reported operation parameter.
 *
 * <p>This legacy runtime test-form model is retained only until the structured
 * GatewayOperationCall migration. The v2 operation definition carries the
 * complete request schema instead of flattening parameters into this model.
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
