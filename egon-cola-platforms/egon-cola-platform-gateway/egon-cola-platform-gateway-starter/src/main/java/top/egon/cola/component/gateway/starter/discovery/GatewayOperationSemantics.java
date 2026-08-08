package top.egon.cola.component.gateway.starter.discovery;

import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.List;

/**
 * Provides null-safe access to optional {@link GatewayOperation} semantics.
 */
final class GatewayOperationSemantics {

    /** Prevents instantiation of this utility class. */
    private GatewayOperationSemantics() {
    }

    /**
     * Returns the tags declared for an operation.
     *
     * @param operation the operation annotation, or {@code null}
     * @return the declared tags, or an empty list when no annotation is present
     */
    static List<String> tags(GatewayOperation operation) {
        return operation == null
                ? List.of()
                : List.of(operation.tags());
    }

    /**
     * Determines whether an operation is explicitly declared idempotent.
     *
     * @param operation the operation annotation, or {@code null}
     * @return {@code true} when the annotation declares idempotency
     */
    static boolean idempotent(GatewayOperation operation) {
        return operation != null && operation.idempotent();
    }
}
