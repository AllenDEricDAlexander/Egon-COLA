package top.egon.cola.platform.tianquan.shoubing.admin.token.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.dto.PublishSigningKeyDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.vo.SigningKeyVO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.SigningKeyService;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

import java.util.List;

@RestController("signingKeyController")
@RequiredArgsConstructor
@Slf4j
@Validated
@RequestMapping("/api/v1/tianquan-shoubing/signing-keys")
@Tag(name = "signing-keys", description = "签名密钥接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份实体域",
        interfaceGroupCode = "tianquan-shoubing-identity"
)

public class SigningKeyController {

    @NonNull
    @Qualifier("signingKeyServiceImpl")
    private final SigningKeyService keys;
    @NonNull
    @Qualifier("idpAdminAuthorizationPort")
    private final IdpAdminAuthorizationPort authorization;
    @NonNull
    @Qualifier("oauthManagementRealmResolver")
    private final OAuthManagementRealmResolver realms;

    @GetMapping
    @Operation(
            operationId = "tianquan-shoubing-signing-key-list-v1",
            summary = "查询签名密钥",
            tags = {"tianquan-shoubing", "signing-key"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public List<SigningKeyVO> list(
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:signing-key:read");
        return keys.list(realms.resolve(principal, realm));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "tianquan-shoubing-signing-key-publish-v1",
            summary = "预发布签名密钥",
            tags = {"tianquan-shoubing", "signing-key"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public SigningKeyVO publish(
            @Valid @RequestBody PublishSigningKeyDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            HttpServletResponse response,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:signing-key:publish");
        TrustedRealmBO trustedRealm = realms.resolve(principal, realm);
        response.setHeader("Cache-Control", "no-store");
        return keys.publish(trustedRealm, request);
    }

    @PostMapping("/{kid}/activate")
    @Operation(
            operationId = "tianquan-shoubing-signing-key-activate-v1",
            summary = "激活签名密钥",
            tags = {"tianquan-shoubing", "signing-key"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public SigningKeyVO activate(
            @PathVariable("kid") String kid,
            @RequestParam("expectedVersion")
            @PositiveOrZero long expectedVersion,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:signing-key:activate");
        return keys.activate(realms.resolve(principal, realm), kid, expectedVersion);
    }

    @PostMapping("/{kid}/retire")
    @Operation(
            operationId = "tianquan-shoubing-signing-key-retire-v1",
            summary = "退役签名密钥",
            tags = {"tianquan-shoubing", "signing-key"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public SigningKeyVO retire(
            @PathVariable("kid") String kid,
            @RequestParam("expectedVersion")
            @PositiveOrZero long expectedVersion,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:signing-key:retire");
        return keys.retire(realms.resolve(principal, realm), kid, expectedVersion);
    }
}
