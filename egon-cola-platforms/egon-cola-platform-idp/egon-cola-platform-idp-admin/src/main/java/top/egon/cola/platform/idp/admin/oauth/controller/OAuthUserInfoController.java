package top.egon.cola.platform.idp.admin.oauth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthUserInfoVO;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.Objects;

/**
 * 向 OAuth 客户端返回当前访问令牌对应的身份声明。
 *
 * <p>Returns identity claims associated with the current access token to OAuth clients.</p>
 */
@RestController
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity-profile",
        entityDomainName = "统一身份本人信息域",
        code = "identity-profile",
        name = "统一身份本人信息接口组")
@EgonHttpService(
        serviceName = "idp-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/")
public class OAuthUserInfoController {

    @GetMapping("/oauth2/userinfo")
    @GatewayOperation(
            name = "idp-oauth-userinfo-v1",
            summary = "查询 OAuth 当前身份声明",
            externalAccessible = true,
            tags = {"idp", "oauth"})
    public OAuthUserInfoVO userInfo(
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        IdentityPrincipal identity = Objects.requireNonNull(
                principal,
                "principal"
        );
        return new OAuthUserInfoVO(
                identity.subject(),
                identity.tenantId(),
                identity.audience()
        );
    }
}
