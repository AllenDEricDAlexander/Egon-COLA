package top.egon.cola.platform.tianquan.shoubing.admin.resource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.BatchResourceServerActionDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.CreateResourceServerDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.ResourceVersionDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

import java.util.List;
import java.util.Objects;

/**
 * Resource Server 管理接口。
 *
 * <p>Administration API for Resource Servers.</p>
 */
@Validated
@RestController
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
    private final ResourceServerService resources;

    /** Tianquan-Jianshen 管理权限闸门；Tianquan-Jianshen administration permission gate. */
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
    @Operation(
            operationId = "tianquan-shoubing-resource-server-list-v1",
            summary = "查询Resource Server",
            tags = {"tianquan-shoubing", "resource-server"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public List<ResourceServerVO> list(
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:read");
        return resources.list();
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
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:read");
        return resources.detail(resourceServerId);
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
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:create");
        return resources.create(request);
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
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:status");
        return resources.enable(resourceServerId, request);
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
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:status");
        return resources.disable(resourceServerId, request);
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
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:status");
        return resources.batch(request);
    }
}
