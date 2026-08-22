package top.egon.cola.platform.idp.admin.resource.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.resource.domain.dto.BatchResourceServerActionDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.CreateResourceServerDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.ResourceVersionDTO;
import top.egon.cola.platform.idp.admin.resource.domain.vo.ResourceServerVO;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.List;
import java.util.Objects;

/**
 * Resource Server 管理接口。
 *
 * <p>Administration API for Resource Servers.</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/identity/resource-servers")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份实体域",
        code = "resource-servers",
        name = "Resource Server接口组")
@EgonHttpService(
        serviceName = "idp-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/v1/identity")
public class ResourceServerController {

    /** Resource Server 管理服务；Resource Server management service. */
    private final ResourceServerService resources;

    /** RBAC3 管理权限闸门；RBAC3 administration permission gate. */
    private final IdpAdminAuthorizationPort authorization;

    /**
     * 创建 Resource Server 管理接口。
     *
     * <p>Creates the Resource Server administration API.</p>
     */
    public ResourceServerController(
            ResourceServerService resources,
            IdpAdminAuthorizationPort authorization
    ) {
        this.resources = Objects.requireNonNull(resources, "resources");
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
    }

    /** @return 全部 Resource Server；all Resource Servers */
    @GetMapping
    @GatewayOperation(name = "idp-resource-server-list-v1",
            summary = "查询Resource Server", externalAccessible = true,
            tags = {"idp", "resource-server"})
    public List<ResourceServerVO> list(
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:resource-server:read");
        return resources.list();
    }

    /**
     * 查询一个 Resource Server。
     *
     * <p>Gets one Resource Server.</p>
     */
    @GetMapping("/{resourceServerId}")
    @GatewayOperation(name = "idp-resource-server-detail-v1",
            summary = "查询Resource Server详情", externalAccessible = true,
            tags = {"idp", "resource-server"})
    public ResourceServerVO detail(
            @PathVariable("resourceServerId") String resourceServerId,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:resource-server:read");
        return resources.detail(resourceServerId);
    }

    /**
     * 创建 Resource Server。
     *
     * <p>Creates a Resource Server.</p>
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @GatewayOperation(name = "idp-resource-server-create-v1",
            summary = "创建Resource Server", externalAccessible = true,
            tags = {"idp", "resource-server"})
    public ResourceServerVO create(
            @Valid @RequestBody CreateResourceServerDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:resource-server:create");
        return resources.create(request);
    }

    /**
     * 启用 Resource Server。
     *
     * <p>Enables a Resource Server.</p>
     */
    @PostMapping("/{resourceServerId}/enable")
    @GatewayOperation(name = "idp-resource-server-enable-v1",
            summary = "启用Resource Server", externalAccessible = true,
            tags = {"idp", "resource-server"})
    public ResourceServerVO enable(
            @PathVariable("resourceServerId") String resourceServerId,
            @Valid @RequestBody ResourceVersionDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:resource-server:status");
        return resources.enable(resourceServerId, request);
    }

    /**
     * 禁用 Resource Server。
     *
     * <p>Disables a Resource Server.</p>
     */
    @PostMapping("/{resourceServerId}/disable")
    @GatewayOperation(name = "idp-resource-server-disable-v1",
            summary = "禁用Resource Server", externalAccessible = true,
            tags = {"idp", "resource-server"})
    public ResourceServerVO disable(
            @PathVariable("resourceServerId") String resourceServerId,
            @Valid @RequestBody ResourceVersionDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:resource-server:status");
        return resources.disable(resourceServerId, request);
    }

    /**
     * 批量修改明确应用的状态。
     *
     * <p>Batch-changes status for explicitly selected applications.</p>
     */
    @PostMapping("/actions/batch")
    @GatewayOperation(name = "idp-resource-server-batch-v1",
            summary = "批量变更Resource Server", externalAccessible = true,
            tags = {"idp", "resource-server"})
    public List<ResourceServerVO> batch(
            @Valid @RequestBody BatchResourceServerActionDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:resource-server:status");
        return resources.batch(request);
    }
}
