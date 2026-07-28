package top.egon.cola.component.gateway.starter.discovery;

import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GatewaySchemaDescriptions {

    private GatewaySchemaDescriptions() {
    }

    static Map<String, Object> apply(
            Map<String, Object> source,
            GatewaySchemaField[] documentation,
            String schemaName) {
        if (documentation.length == 0) {
            return source;
        }
        Map<String, Object> result = copyMap(source);
        for (GatewaySchemaField field : documentation) {
            String path = field.path().trim();
            String description = field.description().trim();
            validate(field, path, description);
            Map<String, Object> node = find(result, path, schemaName);
            if (node.putIfAbsent("description", description) != null) {
                throw new IllegalArgumentException(
                        "duplicate gateway schema field path: " + path
                );
            }
        }
        return result;
    }

    private static void validate(
            GatewaySchemaField field,
            String path,
            String description) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException(
                    "gateway schema field path must not be blank"
            );
        }
        if (description.isEmpty()) {
            throw new IllegalArgumentException(
                    "gateway schema field description must not be blank: "
                            + field.path()
            );
        }
    }

    private static Map<String, Object> find(
            Map<String, Object> schema,
            String path,
            String schemaName) {
        Map<String, Object> current = schema;
        for (String segment : path.split("\\.")) {
            Map<String, Object> properties = properties(current);
            Object next = properties.get(segment);
            if (!(next instanceof Map<?, ?>)) {
                throw new IllegalArgumentException(
                        "gateway schema field path does not exist in "
                                + schemaName + ": " + path
                );
            }
            current = castMap(next);
        }
        return current;
    }

    private static Map<String, Object> properties(
            Map<String, Object> schema) {
        Map<String, Object> current = schema;
        if ("array".equals(current.get("type"))
                && current.get("items") instanceof Map<?, ?> items) {
            current = castMap(items);
        }
        Object properties = current.get("properties");
        return properties instanceof Map<?, ?>
                ? castMap(properties)
                : Map.of();
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(
                String.valueOf(key),
                copyValue(value)
        ));
        return result;
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return copyMap(map);
        }
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            list.forEach(item -> result.add(copyValue(item)));
            return result;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
