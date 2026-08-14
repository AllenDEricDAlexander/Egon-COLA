package top.egon.cola.component.gateway.admin.shared.domain.enums;


/**
 * 中文说明：{@code AdminActorTypeEnum} 是枚举类型，位于当前 Gateway 模块的相关包中，负责ActorType相关的职责与边界。
 * English summary: {@code AdminActorTypeEnum} is an enumeration in the current Gateway module; it owns the actor type-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public enum AdminActorTypeEnum {
    /**
     * 中文说明：表示 USER 这一固定值；它属于 {@code top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value user; it is a state, type, or protocol value of {@code top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum}; do not couple callers to its representation when the owning type exposes an API.
     */
    USER,
    /**
     * 中文说明：表示 服务 这一固定值；它属于 {@code top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value service; it is a state, type, or protocol value of {@code top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum}; do not couple callers to its representation when the owning type exposes an API.
     */
    SERVICE
}
