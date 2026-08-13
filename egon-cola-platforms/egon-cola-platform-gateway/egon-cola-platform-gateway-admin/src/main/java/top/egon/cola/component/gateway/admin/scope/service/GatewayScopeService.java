package top.egon.cola.component.gateway.admin.scope.service;


import top.egon.cola.component.gateway.admin.application.controller.*;
import top.egon.cola.component.gateway.admin.application.domain.dto.*;
import top.egon.cola.component.gateway.admin.application.domain.exception.*;
import top.egon.cola.component.gateway.admin.application.domain.po.*;
import top.egon.cola.component.gateway.admin.application.domain.vo.*;
import top.egon.cola.component.gateway.admin.application.repository.*;
import top.egon.cola.component.gateway.admin.application.service.*;
import top.egon.cola.component.gateway.admin.auth.controller.*;
import top.egon.cola.component.gateway.admin.auth.domain.vo.*;
import top.egon.cola.component.gateway.admin.auth.service.*;
import top.egon.cola.component.gateway.admin.bootstrap.*;
import top.egon.cola.component.gateway.admin.catalog.controller.*;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.*;
import top.egon.cola.component.gateway.admin.catalog.domain.enums.*;
import top.egon.cola.component.gateway.admin.catalog.domain.po.*;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.*;
import top.egon.cola.component.gateway.admin.catalog.repository.*;
import top.egon.cola.component.gateway.admin.catalog.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.catalog.service.*;
import top.egon.cola.component.gateway.admin.config.*;
import top.egon.cola.component.gateway.admin.config.properties.*;
import top.egon.cola.component.gateway.admin.credential.controller.*;
import top.egon.cola.component.gateway.admin.credential.domain.dto.*;
import top.egon.cola.component.gateway.admin.credential.domain.po.*;
import top.egon.cola.component.gateway.admin.credential.domain.vo.*;
import top.egon.cola.component.gateway.admin.credential.repository.*;
import top.egon.cola.component.gateway.admin.credential.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.credential.service.*;
import top.egon.cola.component.gateway.admin.group.controller.*;
import top.egon.cola.component.gateway.admin.group.domain.dto.*;
import top.egon.cola.component.gateway.admin.group.domain.po.*;
import top.egon.cola.component.gateway.admin.group.domain.vo.*;
import top.egon.cola.component.gateway.admin.group.repository.*;
import top.egon.cola.component.gateway.admin.group.service.*;
import top.egon.cola.component.gateway.admin.mcp.controller.*;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.*;
import top.egon.cola.component.gateway.admin.mcp.domain.enums.*;
import top.egon.cola.component.gateway.admin.mcp.domain.exception.*;
import top.egon.cola.component.gateway.admin.mcp.domain.po.*;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.*;
import top.egon.cola.component.gateway.admin.mcp.repository.*;
import top.egon.cola.component.gateway.admin.mcp.repository.filesystem.*;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.mcp.service.*;
import top.egon.cola.component.gateway.admin.observability.controller.*;
import top.egon.cola.component.gateway.admin.observability.controller.message.*;
import top.egon.cola.component.gateway.admin.observability.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.observability.domain.dto.*;
import top.egon.cola.component.gateway.admin.observability.domain.enums.*;
import top.egon.cola.component.gateway.admin.observability.domain.po.*;
import top.egon.cola.component.gateway.admin.observability.domain.vo.*;
import top.egon.cola.component.gateway.admin.observability.repository.*;
import top.egon.cola.component.gateway.admin.observability.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.observability.service.*;
import top.egon.cola.component.gateway.admin.release.controller.*;
import top.egon.cola.component.gateway.admin.release.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.release.domain.*;
import top.egon.cola.component.gateway.admin.release.domain.dto.*;
import top.egon.cola.component.gateway.admin.release.domain.enums.*;
import top.egon.cola.component.gateway.admin.release.domain.po.*;
import top.egon.cola.component.gateway.admin.release.domain.vo.*;
import top.egon.cola.component.gateway.admin.release.repository.*;
import top.egon.cola.component.gateway.admin.release.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.release.service.*;
import top.egon.cola.component.gateway.admin.reporting.controller.openapi.*;
import top.egon.cola.component.gateway.admin.reporting.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.reporting.domain.dto.*;
import top.egon.cola.component.gateway.admin.reporting.domain.po.*;
import top.egon.cola.component.gateway.admin.reporting.domain.vo.*;
import top.egon.cola.component.gateway.admin.reporting.repository.*;
import top.egon.cola.component.gateway.admin.reporting.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.reporting.service.*;
import top.egon.cola.component.gateway.admin.routing.controller.*;
import top.egon.cola.component.gateway.admin.routing.domain.*;
import top.egon.cola.component.gateway.admin.routing.domain.dto.*;
import top.egon.cola.component.gateway.admin.routing.domain.po.*;
import top.egon.cola.component.gateway.admin.routing.domain.vo.*;
import top.egon.cola.component.gateway.admin.routing.repository.*;
import top.egon.cola.component.gateway.admin.routing.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.routing.service.*;
import top.egon.cola.component.gateway.admin.rule.domain.dto.*;
import top.egon.cola.component.gateway.admin.rule.domain.vo.*;
import top.egon.cola.component.gateway.admin.rule.service.*;
import top.egon.cola.component.gateway.admin.runtime.controller.*;
import top.egon.cola.component.gateway.admin.runtime.domain.dto.*;
import top.egon.cola.component.gateway.admin.runtime.domain.vo.*;
import top.egon.cola.component.gateway.admin.runtime.service.*;
import top.egon.cola.component.gateway.admin.scope.controller.*;
import top.egon.cola.component.gateway.admin.scope.domain.*;
import top.egon.cola.component.gateway.admin.scope.domain.dto.*;
import top.egon.cola.component.gateway.admin.scope.domain.vo.*;
import top.egon.cola.component.gateway.admin.scope.service.*;
import top.egon.cola.component.gateway.admin.shared.controller.*;
import top.egon.cola.component.gateway.admin.shared.domain.*;
import top.egon.cola.component.gateway.admin.shared.domain.enums.*;
import top.egon.cola.component.gateway.admin.shared.domain.exception.*;
import top.egon.cola.component.gateway.admin.shared.domain.po.*;
import top.egon.cola.component.gateway.admin.shared.domain.vo.*;
import top.egon.cola.component.gateway.admin.shared.repository.*;
import top.egon.cola.component.gateway.admin.shared.repository.jdbc.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.error.management.DdcManagementClientException;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeBinding;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeQuery;
import top.egon.cola.component.gateway.admin.application.domain.po.GatewayApplicationPO;
import top.egon.cola.component.gateway.admin.application.repository.GatewayApplicationRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO;
import top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey;
import top.egon.cola.component.gateway.admin.scope.domain.vo.GatewayScopeVO;
/**
 * 中文说明：{@code GatewayScopeService} 是服务组件，位于当前 Gateway 模块的相关包中，负责网关Scope服务相关的职责与边界。
 * English summary: {@code GatewayScopeService} is a gateway scope service service in the current Gateway module; it owns the gateway scope service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Service
public class GatewayScopeService {

    /**
     * 中文说明：表示 BINDINGORDER 这一固定值；它属于 {@code GatewayScopeService} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value binding order; it is a state, type, or protocol value of {@code GatewayScopeService} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayScopeService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayScopeService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Comparator<DdcManagementScopeBinding> BINDING_ORDER =
            Comparator.comparing(DdcManagementScopeBinding::bizCode)
                    .thenComparing(DdcManagementScopeBinding::namespaceCode)
                    .thenComparing(DdcManagementScopeBinding::env)
                    .thenComparing(DdcManagementScopeBinding::appCode);

    /**
     * 中文说明：保存 客户端 对应的状态、依赖或配置值；字段类型为 {@code DdcManagementClient}，由 {@code GatewayScopeService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by client; its type is {@code DdcManagementClient}, and {@code GatewayScopeService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayScopeService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayScopeService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DdcManagementClient client;

    /**
     * 中文说明：保存 applications 对应的状态、依赖或配置值；字段类型为 {@code GatewayApplicationRepository}，由 {@code GatewayScopeService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by applications; its type is {@code GatewayApplicationRepository}, and {@code GatewayScopeService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayScopeService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayScopeService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayApplicationRepository applications;

    /**
     * 中文说明：创建 {@code GatewayScopeService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayScopeService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param client 参数 客户端；parameter client。
     * @param applications 参数 applications；parameter applications。
     */
    @Autowired
    public GatewayScopeService(
            ObjectProvider<DdcManagementClient> client,
            GatewayApplicationRepository applications) {
        this(client.getIfAvailable(), applications);
    }

    /**
     * 中文说明：创建 {@code GatewayScopeService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayScopeService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param client 参数 客户端；parameter client。
     * @param applications 参数 applications；parameter applications。
     */
    GatewayScopeService(
            DdcManagementClient client,
            GatewayApplicationRepository applications) {
        this.client = client;
        this.applications = applications;
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code GatewayScopeService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code GatewayScopeService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayScopeService.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    public List<GatewayScopeVO> list() {
        Map<GatewayPhysicalApplicationKey, String> connected = applications
                .findAllByDeletedFalseOrderByCreatedAtDesc().stream()
                .collect(Collectors.toMap(
                        GatewayScopeService::physicalKey,
                        GatewayApplicationPO::getId,
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ));
        return bindings(new GatewayScopeQueryDTO(null, null, null, null)).stream()
                .map(binding -> view(binding, connected))
                .toList();
    }

    /**
     * 中文说明：执行 bindings 操作；该方法是 {@code GatewayScopeService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bindings operation; this method is the invocation entry point on {@code GatewayScopeService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayScopeService.bindings(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 bindings 的处理结果；returns the result of the operation.
     */
    public List<DdcManagementScopeBinding> bindings(GatewayScopeQueryDTO query) {
        Objects.requireNonNull(query, "query");
        try {
            return client().getScopeBindings(new DdcManagementScopeQuery(
                            query.bizCode(),
                            query.namespace(),
                            query.env(),
                            query.appCode()
                    )).stream()
                    .filter(DdcManagementScopeBinding::enabled)
                    .sorted(BINDING_ORDER)
                    .toList();
        } catch (DdcManagementClientException
                 | UnsupportedOperationException error) {
            throw new IllegalStateException(
                    "DDC scope catalog is unavailable",
                    error
            );
        }
    }

    /**
     * 中文说明：执行 requireEnabled 操作；该方法是 {@code GatewayScopeService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require enabled operation; this method is the invocation entry point on {@code GatewayScopeService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayScopeService.requireEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 requireEnabled 的处理结果；returns the result of the operation.
     */
    public DdcManagementScopeBinding requireEnabled(GatewayScopeQueryDTO query) {
        return bindings(query).stream()
                .filter(value -> exact(value, query))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "DDC scope binding is not enabled"
                ));
    }

    /**
     * 中文说明：执行 客户端 操作；该方法是 {@code GatewayScopeService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the client operation; this method is the invocation entry point on {@code GatewayScopeService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayScopeService.client(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 客户端 的处理结果；returns the result of the operation.
     */
    private DdcManagementClient client() {
        if (client == null) {
            throw new IllegalStateException(
                    "DDC management client is not configured"
            );
        }
        return client;
    }

    /**
     * 中文说明：执行 view 操作；该方法是 {@code GatewayScopeService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the view operation; this method is the invocation entry point on {@code GatewayScopeService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayScopeService.view(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param binding 参数 binding；parameter binding。
     * @param connected 参数 connected；parameter connected。
     * @return 返回 view 的处理结果；returns the result of the operation.
     */
    private static GatewayScopeVO view(
            DdcManagementScopeBinding binding,
            Map<GatewayPhysicalApplicationKey, String> connected) {
        String applicationId = connected.get(physicalKey(binding));
        return new GatewayScopeVO(
                binding.bindingId(),
                binding.bizCode(),
                binding.namespaceCode(),
                binding.env(),
                binding.appCode(),
                binding.appName(),
                applicationId != null,
                applicationId
        );
    }

    /**
     * 中文说明：执行 exact 操作；该方法是 {@code GatewayScopeService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the exact operation; this method is the invocation entry point on {@code GatewayScopeService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayScopeService.exact(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param binding 参数 binding；parameter binding。
     * @param query 参数 query；parameter query。
     * @return 返回 exact 的处理结果；returns the result of the operation.
     */
    private static boolean exact(
            DdcManagementScopeBinding binding,
            GatewayScopeQueryDTO query) {
        return Objects.equals(binding.bizCode(), query.bizCode())
                && Objects.equals(binding.namespaceCode(), query.namespace())
                && Objects.equals(binding.env(), query.env())
                && Objects.equals(binding.appCode(), query.appCode());
    }

    /**
     * 中文说明：执行 physical键 操作；该方法是 {@code GatewayScopeService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the physical key operation; this method is the invocation entry point on {@code GatewayScopeService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayScopeService.physicalKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param application 参数 application；parameter application。
     * @return 返回 physical键 的处理结果；returns the result of the operation.
     */
    private static GatewayPhysicalApplicationKey physicalKey(
            GatewayApplicationPO application) {
        return new GatewayPhysicalApplicationKey(
                application.getBizCode(),
                application.getEnv(),
                application.getApplicationCode()
        );
    }

    /**
     * 中文说明：执行 physical键 操作；该方法是 {@code GatewayScopeService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the physical key operation; this method is the invocation entry point on {@code GatewayScopeService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayScopeService.physicalKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param binding 参数 binding；parameter binding。
     * @return 返回 physical键 的处理结果；returns the result of the operation.
     */
    private static GatewayPhysicalApplicationKey physicalKey(
            DdcManagementScopeBinding binding) {
        return new GatewayPhysicalApplicationKey(
                binding.bizCode(),
                binding.env(),
                binding.appCode()
        );
    }






}
