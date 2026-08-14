package top.egon.cola.component.gateway.admin.catalog.domain.vo;


import top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO;
import top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO;

/**
 * 中文说明：{@code GatewayCurrentOperationDefinitionVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Current操作定义相关的职责与边界。
 * English summary: {@code GatewayCurrentOperationDefinitionVO} is an immutable data carrier in the current Gateway module; it owns the current operation definition-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param operation 参数 操作；parameter operation。
 * @param definition 参数 定义；parameter definition。
 */
public record GatewayCurrentOperationDefinitionVO(
        /**
         * 中文说明：保存 操作 对应的状态、依赖或配置值；字段类型为 {@code GatewayOperationPO}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCurrentOperationDefinitionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation; its type is {@code GatewayOperationPO}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCurrentOperationDefinitionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCurrentOperationDefinitionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCurrentOperationDefinitionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayOperationPO operation,
        /**
         * 中文说明：保存 定义 对应的状态、依赖或配置值；字段类型为 {@code GatewayOperationDefinitionPO}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCurrentOperationDefinitionVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by definition; its type is {@code GatewayOperationDefinitionPO}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCurrentOperationDefinitionVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCurrentOperationDefinitionVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCurrentOperationDefinitionVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayOperationDefinitionPO definition
) {
}
