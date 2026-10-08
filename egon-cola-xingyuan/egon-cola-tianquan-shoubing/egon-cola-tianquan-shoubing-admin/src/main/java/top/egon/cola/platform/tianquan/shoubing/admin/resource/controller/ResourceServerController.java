package top.egon.cola.platform.tianquan.shoubing.admin.resource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.BatchResourceServerActionDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.CreateResourceServerDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.ResourceVersionDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

import java.util.List;

/**
 * Resource Server 管理接口。
 *
 * <p>Administration API for Resource Servers.</p>
 */
@Validated
@RestController("resourceServerController")
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/tianquan-shoubing/resource-servers")
@Tag(name = "resource-servers", description = "Resource Server接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份实体域",
        interfaceGroupCode = "tianquan-shoubing-identity"
)

public class ResourceServerController {

    /** Resource Server 管理服务；Resource Server management service. */
    @NonNull
    @Qualifier("resourceServerServiceImpl")
    private final ResourceServerService resources;

    /** Tianquan-Jianshen 管理权限闸门；Tianquan-Jianshen administration permission gate. */
    @NonNull
    @Qualifier("idpAdminAuthorizationPort")
    private final IdpAdminAuthorizationPort authorization;
    @NonNull
    @Qualifier("oauthManagementRealmResolver")
    private final OAuthManagementRealmResolver realms;

    /** @return 全部 Resource Server；all Resource Servers */
    @GetMapping
    @Operation(
            operationId = "tianquan-shoubing-resource-server-list-v1",
            summary = "查询Resource Server",
            tags = {"tianquan-shoubing", "resource-server"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public List<ResourceServerVO> list(
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:read");
        return resources.list(platformRealm(principal, realm));
    }

    /**
     * 查询一个 Resource Server。
     *
     * <p>Gets one Resource Server.</p>
     */
    @GetMapping("/{resourceServerId}")
    @Operation(
            operationId = "tianquan-shoubing-resource-server-detail-v1",
            summary = "查询Resource Server详情",
            tags = {"tianquan-shoubing", "resource-server"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResourceServerVO detail(
            @PathVariable("resourceServerId") String resourceServerId,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:read");
        return resources.detail(platformRealm(principal, realm), resourceServerId);
    }

    /**
     * 创建 Resource Server。
     *
     * <p>Creates a Resource Server.</p>
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "tianquan-shoubing-resource-server-create-v1",
            summary = "创建Resource Server",
            tags = {"tianquan-shoubing", "resource-server"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResourceServerVO create(
            @Valid @RequestBody CreateResourceServerDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:create");
        return resources.create(platformRealm(principal, realm), request);
    }

    /**
     * 启用 Resource Server。
     *
     * <p>Enables a Resource Server.</p>
     */
    @PostMapping("/{resourceServerId}/enable")
    @Operation(
            operationId = "tianquan-shoubing-resource-server-enable-v1",
            summary = "启用Resource Server",
            tags = {"tianquan-shoubing", "resource-server"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResourceServerVO enable(
            @PathVariable("resourceServerId") String resourceServerId,
            @Valid @RequestBody ResourceVersionDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:status");
        return resources.enable(platformRealm(principal, realm), resourceServerId, request);
    }

    /**
     * 禁用 Resource Server。
     *
     * <p>Disables a Resource Server.</p>
     */
    @PostMapping("/{resourceServerId}/disable")
    @Operation(
            operationId = "tianquan-shoubing-resource-server-disable-v1",
            summary = "禁用Resource Server",
            tags = {"tianquan-shoubing", "resource-server"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResourceServerVO disable(
            @PathVariable("resourceServerId") String resourceServerId,
            @Valid @RequestBody ResourceVersionDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:status");
        return resources.disable(platformRealm(principal, realm), resourceServerId, request);
    }

    /**
     * 批量修改明确应用的状态。
     *
     * <p>Batch-changes status for explicitly selected applications.</p>
     */
    @PostMapping("/actions/batch")
    @Operation(
            operationId = "tianquan-shoubing-resource-server-batch-v1",
            summary = "批量变更Resource Server",
            tags = {"tianquan-shoubing", "resource-server"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public List<ResourceServerVO> batch(
            @Valid @RequestBody BatchResourceServerActionDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:status");
        return resources.batch(platformRealm(principal, realm), request);
    }

    private TrustedRealmBO platformRealm(IdentityPrincipal principal, String requestedRealm) {
        TrustedRealmBO trustedRealm = realms.resolve(principal, requestedRealm);
        if (trustedRealm.getRealmType() != RealmTypeEnum.PLATFORM) {
            throw new AccessDeniedException("OAUTH_RESOURCE_DIRECTORY_PLATFORM_REALM_REQUIRED");
        }
        return trustedRealm;
    }
}
