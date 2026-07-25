package top.egon.cola.component.gateway.core.exchange;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Protocol-independent read-only header view.
 */
public interface GatewayHeaders {

    Set<String> names();

    List<String> values(String name);

    default Optional<String> firstValue(String name) {
        List<String> values = values(name);
        return values == null || values.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(values.getFirst());
    }
}
