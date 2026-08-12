package top.egon.cola.component.gateway.admin.interfaces.management;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.scope.GatewayScopeService;

import java.util.List;

/**
 * 中文说明：{@code GatewayScopeController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责网关Scope控制器相关的职责与边界。
 * English summary: {@code GatewayScopeController} is a gateway scope controller controller in the current Gateway module; it owns the gateway scope controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/scopes")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
public class GatewayScopeController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code GatewayScopeService}，由 {@code GatewayScopeController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code GatewayScopeService}, and {@code GatewayScopeController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayScopeController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayScopeController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayScopeService service;

    /**
     * 中文说明：创建 {@code GatewayScopeController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayScopeController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public GatewayScopeController(GatewayScopeService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code GatewayScopeController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code GatewayScopeController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayScopeController.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @GetMapping
    public List<GatewayScopeService.ScopeView> list() {
        return service.list();
    }
}
