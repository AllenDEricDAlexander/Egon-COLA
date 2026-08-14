package top.egon.cola.component.gateway.admin.reporting.domain.vo;


/**
 * 中文说明：{@code GatewayReconcileResultVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GatewayReconcileResultVO相关的职责与边界。
 * English summary: {@code GatewayReconcileResultVO} is an immutable data carrier in the current Gateway module; it owns the reconcile result-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param activatedDefinitionSets 参数 activated定义Sets；parameter activated definition sets。
 * @param retiredDefinitionSets 参数 retired定义Sets；parameter retired definition sets。
 * @param activatedOperations 参数 activatedOperations；parameter activated operations。
 * @param offlinedOperations 参数 offlinedOperations；parameter offlined operations。
 */
public record GatewayReconcileResultVO(
        /**
         * 中文说明：保存 activated定义Sets 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by activated definition sets; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int activatedDefinitionSets,
        /**
         * 中文说明：保存 retired定义Sets 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by retired definition sets; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int retiredDefinitionSets,
        /**
         * 中文说明：保存 activatedOperations 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by activated operations; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int activatedOperations,
        /**
         * 中文说明：保存 offlinedOperations 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by offlined operations; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int offlinedOperations
) {

    /**
     * 中文说明：执行 changed 操作；该方法是 {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the changed operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO.changed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 changed 的处理结果；returns the result of the operation.
     */
    public boolean changed() {
        return activatedDefinitionSets > 0
                || retiredDefinitionSets > 0
                || activatedOperations > 0
                || offlinedOperations > 0;
    }
}
