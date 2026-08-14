package top.egon.cola.component.gateway.admin.reporting.controller.openapi;


/**
 * 中文说明：{@code GatewayReportAuthenticationFailure} 是类型，位于当前 Gateway 模块的相关包中，负责AuthenticationFailure相关的职责与边界。
 * English summary: {@code GatewayReportAuthenticationFailure} is a type in the current Gateway module; it owns the authentication failure-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayReportAuthenticationFailure
        extends RuntimeException {

    /**
     * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayReportAuthenticationFailure} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayReportAuthenticationFailure} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayReportAuthenticationFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayReportAuthenticationFailure}; do not couple callers to its representation when the owning type exposes an API.
     */
    final int status;

    /**
     * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayReportAuthenticationFailure} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayReportAuthenticationFailure} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayReportAuthenticationFailure} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayReportAuthenticationFailure}; do not couple callers to its representation when the owning type exposes an API.
     */
    final String code;

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayReportAuthenticationFailure} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.reporting.controller.openapi.GatewayReportAuthenticationFailure} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param status 参数 status；parameter status。
     * @param code 参数 code；parameter code。
     */
    GatewayReportAuthenticationFailure(int status, String code) {
        super("gateway report authentication failed");
        this.status = status;
        this.code = code;
    }
}
