package top.egon.cola.component.gateway.admin.shared.domain.vo;


import java.time.Instant;
import java.util.List;


/**
 * 中文说明：{@code GatewayAdminErrorVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Error响应相关的职责与边界。
 * English summary: {@code GatewayAdminErrorVO} is an immutable data carrier in the current Gateway module; it owns the error response-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param code 参数 code；parameter code。
 * @param message 参数 消息；parameter message。
 * @param currentRevision 参数 currentRevision；parameter current revision。
 * @param errors 参数 errors；parameter errors。
 * @param timestamp 参数 timestamp；parameter timestamp。
 */
public record GatewayAdminErrorVO(
        /**
         * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String code,
        /**
         * 中文说明：保存 消息 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by message; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String message,
        /**
         * 中文说明：保存 currentRevision 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by current revision; its type is {@code Long}, and {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Long currentRevision,
        /**
         * 中文说明：保存 errors 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayAdminFieldErrorVO>}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by errors; its type is {@code List<GatewayAdminFieldErrorVO>}, and {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<GatewayAdminFieldErrorVO> errors,
        /**
         * 中文说明：保存 timestamp 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by timestamp; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.vo.GatewayAdminErrorVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant timestamp
) {
}
