package top.egon.cola.component.gateway.admin.catalog.domain.vo;


import java.util.List;

/**
 * 中文说明：{@code GatewayCatalogTreeVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责目录Tree相关的职责与边界。
 * English summary: {@code GatewayCatalogTreeVO} is an immutable data carrier in the current Gateway module; it owns the catalog tree-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param applicationId 参数 applicationId；parameter application id。
 * @param businessDomains 参数 businessDomains；parameter business domains。
 */
public record GatewayCatalogTreeVO(
        /**
         * 中文说明：保存 applicationId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCatalogTreeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by application id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCatalogTreeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCatalogTreeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCatalogTreeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String applicationId,
        /**
         * 中文说明：保存 businessDomains 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayBusinessNodeVO>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCatalogTreeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by business domains; its type is {@code List<GatewayBusinessNodeVO>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCatalogTreeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCatalogTreeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCatalogTreeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<GatewayBusinessNodeVO> businessDomains
) {
}
