package top.egon.cola.platform.idp.admin.resource.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.resource.domain.dto.BatchClientResourceGrantDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.DeleteClientResourceGrantDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.UpsertClientResourceGrantDTO;
import top.egon.cola.platform.idp.admin.resource.domain.vo.ClientResourceGrantVO;
import top.egon.cola.platform.idp.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.List;
import java.util.Objects;

/**
 * OAuth Client 到 Resource Server 的显式授权管理接口。
 *
 * <p>Administration API for explicit OAuth Client-to-Resource Server grants.</p>
 */
@RestController
@RequestMapping("/api/v1/identity/clients")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份实体域",
        code = "client-resource-grants",
        name = "Client Resource Grant接口组")
@EgonHttpService(
        serviceName = "idp-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/v1/identity")
public class ClientResourceGrantController {

    /** Resource Server 管理服务；Resource Server management service. */
    private final ResourceServerService resources;

    /** RBAC3 管理权限闸门；RBAC3 administration permission gate. */
    private final IdpAdminAuthorizationPort authorization;

    /**
     * 创建 Client Resource Grant 管理接口。
     *
     * <p>Creates the Client Resource Grant administration API.</p>
     */
    public ClientResourceGrantController(
            ResourceServerService resources,
            IdpAdminAuthorizationPort authorization
    ) {
        this.resources = Objects.requireNonNull(resources, "resources");
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
    }

    /**
     * 新建或更新一个应用级 Grant。
     *
     * <p>Creates or updates one application-level Grant.</p>
     */
    @PutMapping("/{clientId}/resources/{resourceServerId}")
    @GatewayOperation(name = "idp-client-resource-grant-put-v1",
            summary = "登记Client Resource Grant", externalAccessible = true,
            tags = {"idp", "resource-server"})
    public ClientResourceGrantVO put(
            @PathVariable("clientId") String clientId,
            @PathVariable("resourceServerId") String resourceServerId,
            @Valid @RequestBody UpsertClientResourceGrantDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:resource-server:grant");
        return resources.putGrant(clientId, resourceServerId, request);
    }

    /**
     * 删除一个应用级 Grant。
     *
     * <p>Deletes one application-level Grant.</p>
     */
    @DeleteMapping("/{clientId}/resources/{resourceServerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @GatewayOperation(name = "idp-client-resource-grant-delete-v1",
            summary = "删除Client Resource Grant", externalAccessible = true,
            tags = {"idp", "resource-server"})
    public void delete(
            @PathVariable("clientId") String clientId,
            @PathVariable("resourceServerId") String resourceServerId,
            @Valid @RequestBody DeleteClientResourceGrantDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:resource-server:grant");
        resources.deleteGrant(clientId, resourceServerId, request);
    }

    /**
     * 批量增删明确应用的 Grant。
     *
     * <p>Batch-adds or deletes Grants for explicitly selected applications.</p>
     */
    @PostMapping("/{clientId}/resource-grants/actions/batch")
    @GatewayOperation(name = "idp-client-resource-grant-batch-v1",
            summary = "批量变更Client Resource Grant", externalAccessible = true,
            tags = {"idp", "resource-server"})
    public List<ClientResourceGrantVO> batch(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody BatchClientResourceGrantDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:resource-server:grant");
        return resources.batchGrants(clientId, request);
    }
}
