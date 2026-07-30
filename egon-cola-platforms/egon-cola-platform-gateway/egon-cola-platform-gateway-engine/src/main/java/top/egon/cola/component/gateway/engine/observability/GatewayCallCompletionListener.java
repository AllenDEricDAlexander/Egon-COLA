package top.egon.cola.component.gateway.engine.observability;

import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.util.Arrays;
import java.util.List;

@FunctionalInterface
public interface GatewayCallCompletionListener {

    void onComplete(GatewayCallEventV1 event);

    static GatewayCallCompletionListener noop() {
        return event -> {
        };
    }

    static GatewayCallCompletionListener composite(
            GatewayCallCompletionListener... listeners) {
        List<GatewayCallCompletionListener> snapshot =
                Arrays.stream(listeners)
                        .filter(listener -> listener != null)
                        .toList();
        return event -> snapshot.forEach(listener -> {
            try {
                listener.onComplete(event);
            } catch (RuntimeException ignored) {
                // An observability sink must never change the business result.
            }
        });
    }
}
