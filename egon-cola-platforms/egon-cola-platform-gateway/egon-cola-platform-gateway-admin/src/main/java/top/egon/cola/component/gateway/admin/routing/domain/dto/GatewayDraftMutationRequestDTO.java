package top.egon.cola.component.gateway.admin.routing.domain.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;


/**
 * 中文说明：{@code GatewayDraftMutationRequestDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Mutation请求相关的职责与边界。
 * English summary: {@code GatewayDraftMutationRequestDTO} is an immutable data carrier in the current Gateway module; it owns the mutation request-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param expectedRevision 参数 expectedRevision；parameter expected revision。
 * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
 * @param changeReason 参数 changeReason；parameter change reason。
 */
public record GatewayDraftMutationRequestDTO(
        /**
         * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @PositiveOrZero long expectedRevision,
        /**
         * 中文说明：保存 idempotency键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by idempotency key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String idempotencyKey,
        /**
         * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String changeReason
) {

    /**
     * 中文说明：执行 control 操作；该方法是 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the control operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO.control(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 control 的处理结果；returns the result of the operation.
     */
    public top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO control() {
        return new top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO(
                expectedRevision,
                idempotencyKey,
                changeReason
        );
    }
}
