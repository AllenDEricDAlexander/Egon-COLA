package top.egon.cola.component.gateway.admin.routing.domain.vo;


import java.time.Instant;
import java.util.List;

/**
 * 中文说明：{@code GatewayDraftVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责草稿View相关的职责与边界。
 * English summary: {@code GatewayDraftVO} is an immutable data carrier in the current Gateway module; it owns the draft view-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param revision 参数 revision；parameter revision。
 * @param basedOnReleaseId 参数 basedOn发布Id；parameter based on release id。
 * @param status 参数 status；parameter status。
 * @param changeSummary 参数 changeSummary；parameter change summary。
 * @param routes 参数 routes；parameter routes。
 * @param policies 参数 policies；parameter policies。
 * @param updatedAt 参数 updatedAt；parameter updated at。
 */
public record GatewayDraftVO(
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long revision,
        /**
         * 中文说明：保存 basedOn发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by based on release id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String basedOnReleaseId,
        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String status,
        /**
         * 中文说明：保存 changeSummary 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change summary; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeSummary,
        /**
         * 中文说明：保存 routes 对应的状态、依赖或配置值；字段类型为 {@code List<top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO>}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by routes; its type is {@code List<top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO>}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO> routes,
        /**
         * 中文说明：保存 policies 对应的状态、依赖或配置值；字段类型为 {@code List<top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO>}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policies; its type is {@code List<top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO>}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO> policies,
        /**
         * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant updatedAt
) {
}
