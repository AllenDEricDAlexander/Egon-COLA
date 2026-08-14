package top.egon.cola.component.gateway.admin.observability.domain.vo;


import java.util.List;

/**
 * 中文说明：{@code GatewayPageVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GatewayPageVO相关的职责与边界。
 * English summary: {@code GatewayPageVO} is an immutable data carrier in the current Gateway module; it owns the page-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param items 参数 items；parameter items。
 * @param page 参数 page；parameter page。
 * @param size 参数 size；parameter size。
 * @param total 参数 total；parameter total。
 */
public record GatewayPageVO<T>(
/**
 * 中文说明：保存 items 对应的状态、依赖或配置值；字段类型为 {@code List<T>}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} 在其生命周期内读取或更新。
 * English summary: Holds the state, dependency, or configuration represented by items; its type is {@code List<T>}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} reads or updates it during its lifecycle.
 *
 * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO}; do not couple callers to its representation when the owning type exposes an API.
 */
List<T> items,
/**
 * 中文说明：保存 page 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} 在其生命周期内读取或更新。
 * English summary: Holds the state, dependency, or configuration represented by page; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} reads or updates it during its lifecycle.
 *
 * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO}; do not couple callers to its representation when the owning type exposes an API.
 */
int page,
/**
 * 中文说明：保存 size 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} 在其生命周期内读取或更新。
 * English summary: Holds the state, dependency, or configuration represented by size; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} reads or updates it during its lifecycle.
 *
 * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO}; do not couple callers to its representation when the owning type exposes an API.
 */
int size,
/**
 * 中文说明：保存 total 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} 在其生命周期内读取或更新。
 * English summary: Holds the state, dependency, or configuration represented by total; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} reads or updates it during its lifecycle.
 *
 * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO}; do not couple callers to its representation when the owning type exposes an API.
 */
long total) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param items 参数 items；parameter items。
     * @param page 参数 page；parameter page。
     * @param size 参数 size；parameter size。
     * @param total 参数 total；parameter total。
     */
    public GatewayPageVO {
        items = List.copyOf(items);
    }
}
