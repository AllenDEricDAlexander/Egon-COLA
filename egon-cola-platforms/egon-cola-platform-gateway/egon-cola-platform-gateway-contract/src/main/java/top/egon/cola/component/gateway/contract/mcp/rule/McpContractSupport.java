package top.egon.cola.component.gateway.contract.mcp.rule;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * MCP 契约记录共用的规范化和校验工具。
 *
 * <p>该类仅供规则包内部使用，统一保证列表、集合和映射在发布前具有稳定顺序及不可变语义。
 */
final class McpContractSupport {

    private McpContractSupport() {
    }

    static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static long nonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    static int positive(int value, String field) {
        if (value < 1) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    static <T> List<T> sorted(
            List<T> values,
            Comparator<T> comparator) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(value -> Objects.requireNonNull(value, "MCP rule item"))
                .sorted(comparator)
                .toList();
    }

    static List<String> sortedStrings(List<String> values) {
        return sorted(
                values,
                Comparator.naturalOrder()
        ).stream().map(value -> required(value, "list item")).toList();
    }

    static Set<String> sortedStrings(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = values.stream()
                .map(value -> required(value, "set item"))
                .sorted()
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new
                ));
        return Collections.unmodifiableSet(normalized);
    }

    static <E extends Enum<E>> Set<E> sortedEnums(Set<E> values) {
        if (values == null) {
            return Set.of();
        }
        LinkedHashSet<E> normalized = values.stream()
                .map(value -> Objects.requireNonNull(value, "enum item"))
                .sorted(Comparator.comparing(Enum::name))
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new
                ));
        return Collections.unmodifiableSet(normalized);
    }

    static Map<String, String> sortedMap(Map<String, String> values) {
        if (values == null) {
            return Map.of();
        }
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> normalized.put(
                        required(entry.getKey(), "map key"),
                        Objects.requireNonNull(entry.getValue(), "map value")
                ));
        return Collections.unmodifiableMap(normalized);
    }
}
