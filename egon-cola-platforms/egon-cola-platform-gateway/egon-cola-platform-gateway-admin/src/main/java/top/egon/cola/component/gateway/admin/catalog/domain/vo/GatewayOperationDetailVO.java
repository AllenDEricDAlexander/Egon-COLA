package top.egon.cola.component.gateway.admin.catalog.domain.vo;


import java.util.List;

/**
 * 中文说明：{@code GatewayOperationDetailVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责操作Detail相关的职责与边界。
 * English summary: {@code GatewayOperationDetailVO} is an immutable data carrier in the current Gateway module; it owns the operation detail-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param operation 参数 操作；parameter operation。
 * @param definitions 参数 definitions；parameter definitions。
 */
public record GatewayOperationDetailVO(
        /**
         * 中文说明：保存 操作 对应的状态、依赖或配置值；字段类型为 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation; its type is {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO operation,
        /**
         * 中文说明：保存 definitions 对应的状态、依赖或配置值；字段类型为 {@code List<top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by definitions; its type is {@code List<top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO> definitions
) {
}
