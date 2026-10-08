package top.egon.cola.platform.tianquan.shoubing.admin.resource.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.BatchClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.DeleteClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.UpsertClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ClientResourceGrantVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

import java.util.List;
import java.util.Map;

/**
 * OAuth Client 到 Resource Server 的显式授权管理接口。
 *
 * <p>Administration API for explicit OAuth Client-to-Resource Server grants.</p>
 */
@RestController("clientResourceGrantController")
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/tianquan-shoubing/clients")
@Tag(name = "client-resource-grants", description = "Client Resource Grant接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份实体域",
        interfaceGroupCode = "tianquan-shoubing-identity"
)

public class ClientResourceGrantController {

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

    /**
     * 创建 Client Resource Grant 管理接口。
     *
     * <p>Creates the Client Resource Grant administration API.</p>
     */
    @GetMapping("/{clientId}/resources")
    @Operation(
            operationId = "tianquan-shoubing-client-resource-grant-list-v1",
            summary = "查询Client Resource Grant",
            tags = {"tianquan-shoubing", "resource-server"}
    )
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    public ResponseEntity<?> list(
            @PathVariable("clientId") String clientId,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:grant");
        TrustedRealmBO clientRealm = realms.resolve(principal, realm);
        List<ClientResourceGrantVO> grants = resources.listGrants(clientRealm, realms.platformRealm(), clientId);
        if (grants.size() > 64) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(Map.of("code", "TEMPORARILY_UNAVAILABLE", "message", "Grant list is temporarily unavailable"));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(grants);
    }

    /**
     * 新建或更新一个应用级 Grant。
     *
     * <p>Creates or updates one application-level Grant.</p>
     */
    @PutMapping("/{clientId}/resources/{resourceServerId}")
    @Operation(
            operationId = "tianquan-shoubing-client-resource-grant-put-v1",
            summary = "登记Client Resource Grant",
            tags = {"tianquan-shoubing", "resource-server"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ClientResourceGrantVO put(
            @PathVariable("clientId") String clientId,
            @PathVariable("resourceServerId") String resourceServerId,
            @Valid @RequestBody UpsertClientResourceGrantDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:grant");
        TrustedRealmBO clientRealm = realms.resolve(principal, realm);
        return resources.putGrant(clientRealm, realms.platformRealm(), clientId, resourceServerId, request);
    }

    /**
     * 删除一个应用级 Grant。
     *
     * <p>Deletes one application-level Grant.</p>
     */
    @DeleteMapping("/{clientId}/resources/{resourceServerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            operationId = "tianquan-shoubing-client-resource-grant-delete-v1",
            summary = "删除Client Resource Grant",
            tags = {"tianquan-shoubing", "resource-server"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public void delete(
            @PathVariable("clientId") String clientId,
            @PathVariable("resourceServerId") String resourceServerId,
            @Valid @RequestBody DeleteClientResourceGrantDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:grant");
        TrustedRealmBO clientRealm = realms.resolve(principal, realm);
        resources.deleteGrant(clientRealm, realms.platformRealm(), clientId, resourceServerId, request);
    }

    /**
     * 批量增删明确应用的 Grant。
     *
     * <p>Batch-adds or deletes Grants for explicitly selected applications.</p>
     */
    @PostMapping("/{clientId}/resource-grants/actions/batch")
    @Operation(
            operationId = "tianquan-shoubing-client-resource-grant-batch-v1",
            summary = "批量变更Client Resource Grant",
            tags = {"tianquan-shoubing", "resource-server"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public List<ClientResourceGrantVO> batch(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody BatchClientResourceGrantDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:resource-server:grant");
        TrustedRealmBO clientRealm = realms.resolve(principal, realm);
        return resources.batchGrants(clientRealm, realms.platformRealm(), clientId, request);
    }
}
