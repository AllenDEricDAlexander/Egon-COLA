package top.egon.cola.component.gateway.mcp.task;

import org.reactivestreams.Publisher;

import java.util.Map;
import java.util.Objects;

@FunctionalInterface
public interface McpTaskExecutor {

    Publisher<Outcome> execute(McpTask task);

    record Outcome(
            Type type,
            String inputRequestKey,
            Map<String, Object> payload
    ) {

        public Outcome {
            type = Objects.requireNonNull(type, "type");
            inputRequestKey = inputRequestKey == null
                    || inputRequestKey.isBlank()
                    ? null
                    : inputRequestKey.trim();
            payload = payload == null ? Map.of() : Map.copyOf(payload);
            if ((type == Type.INPUT_REQUIRED)
                    != (inputRequestKey != null)) {
                throw new IllegalArgumentException(
                        "inputRequestKey is required only for input"
                );
            }
        }

        public static Outcome completed(Map<String, Object> result) {
            return new Outcome(Type.COMPLETED, null, result);
        }

        public static Outcome inputRequired(
                String inputRequestKey,
                Map<String, Object> request) {
            return new Outcome(Type.INPUT_REQUIRED, inputRequestKey, request);
        }

        public static Outcome failed(Map<String, Object> error) {
            return new Outcome(Type.FAILED, null, error);
        }
    }

    enum Type {
        COMPLETED,
        INPUT_REQUIRED,
        FAILED
    }
}
