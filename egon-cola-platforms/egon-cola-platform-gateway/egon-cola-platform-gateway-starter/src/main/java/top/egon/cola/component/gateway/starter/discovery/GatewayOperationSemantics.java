package top.egon.cola.component.gateway.starter.discovery;

import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.List;

/**
 * Provides null-safe access to optional {@link GatewayOperation} semantics.
 *
 * 为可选的 {@link GatewayOperation} 语义提供空值安全的访问方法。
 */
final class GatewayOperationSemantics {

    /** Prevents instantiation of this utility class. 防止实例化此工具类。 */
    private GatewayOperationSemantics() {
    }

    /**
     * Returns the tags declared for an operation.
     *
     * 返回操作声明的标签。
     *
     * @param operation the operation annotation, or {@code null}，操作注解，可为 {@code null}
     * @return the declared tags, or an empty list when no annotation is present，声明的标签；没有注解时返回空列表
     */
    static List<String> tags(GatewayOperation operation) {
        return operation == null
                ? List.of()
                : List.of(operation.tags());
    }

    /**
     * Determines whether an operation is explicitly declared idempotent.
     *
     * 判断操作是否被显式声明为幂等。
     *
     * @param operation the operation annotation, or {@code null}，操作注解，可为 {@code null}
     * @return {@code true} when the annotation declares idempotency，注解声明幂等时返回 {@code true}
     */
    static boolean idempotent(GatewayOperation operation) {
        return operation != null && operation.idempotent();
    }
}
