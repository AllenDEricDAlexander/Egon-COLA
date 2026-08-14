package top.egon.cola.component.gateway.admin.reporting.controller.openapi;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.reporting.service.GatewayDefinitionReportService;
import top.egon.cola.component.gateway.admin.reporting.service.GatewayReportAuthentication;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

/**
 * 中文说明：{@code GatewayDefinitionReportController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责网关定义报告控制器相关的职责与边界。
 * English summary: {@code GatewayDefinitionReportController} is a gateway definition report controller controller in the current Gateway module; it owns the gateway definition report controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping(
        "/api/v1/gateway/openapi/interface-definitions/reports"
)
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "gateway-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        code = "gateway-admin-gateway-definition-report-controller",
        name = "GatewayDefinitionReportController 管理接口组")
public class GatewayDefinitionReportController {

    /**
     * 中文说明：保存 reports 对应的状态、依赖或配置值；字段类型为 {@code GatewayDefinitionReportService}，由 {@code GatewayDefinitionReportController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by reports; its type is {@code GatewayDefinitionReportService}, and {@code GatewayDefinitionReportController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionReportController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionReportController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDefinitionReportService reports;

    /**
     * 中文说明：创建 {@code GatewayDefinitionReportController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDefinitionReportController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param reports 参数 reports；parameter reports。
     */
    public GatewayDefinitionReportController(
            GatewayDefinitionReportService reports) {
        this.reports = reports;
    }

    /**
     * 中文说明：执行 报告 操作；该方法是 {@code GatewayDefinitionReportController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the report operation; this method is the invocation entry point on {@code GatewayDefinitionReportController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportController.report(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param report 参数 报告；parameter report。
     * @param reportId 参数 报告Id；parameter report id。
     * @param contractVersion 参数 contractVersion；parameter contract version。
     * @param request 参数 请求；parameter request。
     * @return 返回 报告 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GatewayInterfaceDefinitionReportResult report(
            @RequestBody GatewayInterfaceDefinitionReport report,
            @RequestHeader("X-Gateway-Report-Id") String reportId,
            @RequestHeader("X-Gateway-Contract-Version")
            String contractVersion,
            HttpServletRequest request) {
        return reports.accept(
                authentication(request),
                report,
                reportId,
                contractVersion
        );
    }

    /**
     * 中文说明：执行 find 操作；该方法是 {@code GatewayDefinitionReportController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code GatewayDefinitionReportController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportController.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param reportId 参数 报告Id；parameter report id。
     * @param request 参数 请求；parameter request。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @GetMapping("/{reportId}")
    public GatewayInterfaceDefinitionReportResult find(
            @PathVariable String reportId,
            HttpServletRequest request) {
        return reports.find(authentication(request), reportId);
    }

    /**
     * 中文说明：执行 authentication 操作；该方法是 {@code GatewayDefinitionReportController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authentication operation; this method is the invocation entry point on {@code GatewayDefinitionReportController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportController.authentication(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 authentication 的处理结果；returns the result of the operation.
     */
    private GatewayReportAuthentication authentication(
            HttpServletRequest request) {
        Object value = request.getAttribute(
                GatewayReportAuthentication.REQUEST_ATTRIBUTE
        );
        if (value instanceof GatewayReportAuthentication authentication) {
            return authentication;
        }
        throw new IllegalStateException(
                "GATEWAY_REPORT_AUTHENTICATION_REQUIRED"
        );
    }
}
