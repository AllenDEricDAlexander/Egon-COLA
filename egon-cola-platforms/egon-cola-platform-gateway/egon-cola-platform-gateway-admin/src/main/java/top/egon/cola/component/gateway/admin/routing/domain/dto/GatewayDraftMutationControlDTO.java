package top.egon.cola.component.gateway.admin.routing.domain.dto;


/**
 * 中文说明：{@code GatewayDraftMutationControlDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MutationControl相关的职责与边界。
 * English summary: {@code GatewayDraftMutationControlDTO} is an immutable data carrier in the current Gateway module; it owns the mutation control-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param expectedRevision 参数 expectedRevision；parameter expected revision。
 * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
 * @param changeReason 参数 changeReason；parameter change reason。
 */
public record GatewayDraftMutationControlDTO(
        /**
         * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long expectedRevision,
        /**
         * 中文说明：保存 idempotency键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by idempotency key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String idempotencyKey,
        /**
         * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeReason
) {
}
