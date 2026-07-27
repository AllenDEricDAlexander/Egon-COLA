package top.egon.cola.component.gateway.admin.rule;

import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps reported operation parameters onto the lean runtime model published in
 * gateway rules.
 *
 * <p>The definition report keeps its parameters under the {@link #ATTRIBUTE_KEY}
 * entry of an operation definition's attribute map (written by
 * {@code JdbcGatewayDefinitionReportStore}). Depending on whether that map has
 * been through the JSONB column, an entry is either a report
 * {@code Parameter} record or its generic {@code Map} form, so both are
 * accepted.
 *
 * <p>Entries that carry no name or no location are dropped rather than
 * rejected: parameter metadata drives an optional admin test form, and a
 * malformed one must not fail a release.
 */
public final class GatewayRuntimeParameterMapper {

    public static final String ATTRIBUTE_KEY = "parameters";

    private GatewayRuntimeParameterMapper() {
    }

    public static List<GatewayRuntimeParameter> map(Object reported) {
        if (!(reported instanceof List<?> values)) {
            return List.of();
        }
        List<GatewayRuntimeParameter> parameters = new ArrayList<>();
        for (Object value : values) {
            GatewayRuntimeParameter parameter = parameter(value);
            if (parameter != null) {
                parameters.add(parameter);
            }
        }
        return List.copyOf(parameters);
    }

    private static GatewayRuntimeParameter parameter(Object value) {
        if (value instanceof GatewayInterfaceDefinitionReport.Parameter param) {
            return of(
                    param.name(),
                    param.location(),
                    param.required(),
                    param.javaTypeDisplay(),
                    param.defaultValue(),
                    param.description()
            );
        }
        if (value instanceof Map<?, ?> param) {
            return of(
                    text(param.get("name")),
                    text(param.get("location")),
                    flag(param.get("required")),
                    text(param.get("javaTypeDisplay")),
                    text(param.get("defaultValue")),
                    text(param.get("description"))
            );
        }
        return null;
    }

    private static GatewayRuntimeParameter of(
            String name,
            String location,
            boolean required,
            String typeDisplay,
            String defaultValue,
            String description) {
        if (name == null || name.isBlank()
                || location == null || location.isBlank()) {
            return null;
        }
        return new GatewayRuntimeParameter(
                name,
                location,
                required,
                typeDisplay,
                defaultValue,
                description
        );
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static boolean flag(Object value) {
        return value instanceof Boolean flag
                ? flag
                : Boolean.parseBoolean(text(value));
    }
}
