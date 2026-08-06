package top.egon.cola.component.gateway.starter.discovery;

import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.List;

final class GatewayOperationSemantics {

    private GatewayOperationSemantics() {
    }

    static List<String> tags(GatewayOperation operation) {
        return operation == null
                ? List.of()
                : List.of(operation.tags());
    }

    static boolean idempotent(GatewayOperation operation) {
        return operation != null && operation.idempotent();
    }
}
