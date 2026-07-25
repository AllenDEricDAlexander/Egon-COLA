package top.egon.cola.component.gateway.starter.discovery;

import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.Arrays;
import java.util.List;

final class GatewayOperationSemantics {

    private static final String IDEMPOTENT_TAG = "idempotent";

    private GatewayOperationSemantics() {
    }

    static List<String> tags(GatewayOperation operation) {
        return operation == null
                ? List.of()
                : List.of(operation.tags());
    }

    static boolean idempotent(GatewayOperation operation) {
        return operation != null && Arrays.stream(operation.tags())
                .anyMatch(IDEMPOTENT_TAG::equalsIgnoreCase);
    }
}
