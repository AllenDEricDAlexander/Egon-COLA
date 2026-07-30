package top.egon.cola.component.gateway.core.exchange;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ImmutableGatewayHeaders implements GatewayHeaders {

    private static final ImmutableGatewayHeaders EMPTY =
            new ImmutableGatewayHeaders(Map.of());

    private final Map<String, List<String>> values;

    public ImmutableGatewayHeaders(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((name, headerValues) -> copy.put(
                    name.toLowerCase(Locale.ROOT),
                    List.copyOf(headerValues)
            ));
        }
        values = Map.copyOf(copy);
    }

    public static ImmutableGatewayHeaders empty() {
        return EMPTY;
    }

    @Override
    public Set<String> names() {
        return values.keySet();
    }

    @Override
    public List<String> values(String name) {
        if (name == null) {
            return List.of();
        }
        return values.getOrDefault(name.toLowerCase(Locale.ROOT), List.of());
    }

    public Map<String, List<String>> asMap() {
        return values;
    }
}
