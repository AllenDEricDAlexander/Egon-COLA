package top.egon.cola.component.gateway.admin.release.domain.dto;


/**
 * 中文说明：{@code GatewayReleaseCreateCommandDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Create发布相关的职责与边界。
 * English summary: {@code GatewayReleaseCreateCommandDTO} is an immutable data carrier in the current Gateway module; it owns the create release-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
 * @param changeReason 参数 changeReason；parameter change reason。
 */
public record GatewayReleaseCreateCommandDTO(
        /**
         * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseCreateCommandDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseCreateCommandDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseCreateCommandDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseCreateCommandDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long expectedDraftRevision,
        /**
         * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseCreateCommandDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseCreateCommandDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseCreateCommandDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseCreateCommandDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeReason
) {
}
