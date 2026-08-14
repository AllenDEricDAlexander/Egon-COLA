package top.egon.cola.component.gateway.admin.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationBootstrapService;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import java.util.Objects;

/**
 * Unified SSO bootstrap endpoint for the Gateway administration web application.
 * 补充说明 / Supplementary summary: {@code GatewayAuthBootstrapController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责网关认证Bootstrap控制器相关的职责与边界。
 * English supplement: {@code GatewayAuthBootstrapController} is a gateway auth bootstrap controller controller in the current Gateway module; it owns the gateway auth bootstrap controller-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/auth")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "gateway-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        code = "gateway-admin-gateway-auth-bootstrap-controller",
        name = "GatewayAuthBootstrapController 管理接口组")
public class GatewayAuthBootstrapController {

    /**
     * 中文说明：保存 bootstrap 对应的状态、依赖或配置值；字段类型为 {@code AuthorizationBootstrapService}，由 {@code GatewayAuthBootstrapController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by bootstrap; its type is {@code AuthorizationBootstrapService}, and {@code GatewayAuthBootstrapController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAuthBootstrapController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAuthBootstrapController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AuthorizationBootstrapService bootstrap;

    /**
     * 中文说明：创建 {@code GatewayAuthBootstrapController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayAuthBootstrapController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param bootstrap 参数 bootstrap；parameter bootstrap。
     */
    public GatewayAuthBootstrapController(AuthorizationBootstrapService bootstrap) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
    }

    /**
     * 中文说明：执行 bootstrap 操作；该方法是 {@code GatewayAuthBootstrapController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bootstrap operation; this method is the invocation entry point on {@code GatewayAuthBootstrapController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAuthBootstrapController.bootstrap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 bootstrap 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @GetMapping("/bootstrap")
    @RequiresPermission("gateway:read")
    public BootstrapView bootstrap() {
        return bootstrap.current();
    }
}
