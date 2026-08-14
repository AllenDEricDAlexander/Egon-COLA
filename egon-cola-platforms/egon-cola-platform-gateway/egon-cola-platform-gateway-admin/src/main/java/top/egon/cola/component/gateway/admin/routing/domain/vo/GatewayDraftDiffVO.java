package top.egon.cola.component.gateway.admin.routing.domain.vo;


/**
 * 中文说明：{@code GatewayDraftDiffVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责草稿Diff相关的职责与边界。
 * English summary: {@code GatewayDraftDiffVO} is an immutable data carrier in the current Gateway module; it owns the draft diff-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param basedOnReleaseId 参数 basedOn发布Id；parameter based on release id。
 * @param revision 参数 revision；parameter revision。
 * @param routeCount 参数 路由Count；parameter route count。
 * @param policyCount 参数 策略Count；parameter policy count。
 * @param draftSha256 参数 草稿Sha256；parameter draft sha256。
 */
public record GatewayDraftDiffVO(
        /**
         * 中文说明：保存 basedOn发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by based on release id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String basedOnReleaseId,
        /**
         * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long revision,
        /**
         * 中文说明：保存 路由Count 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by route count; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int routeCount,
        /**
         * 中文说明：保存 策略Count 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy count; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int policyCount,
        /**
         * 中文说明：保存 草稿Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by draft sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String draftSha256
) {
}
