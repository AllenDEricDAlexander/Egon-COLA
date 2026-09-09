package top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthUserInfoVO;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

import java.util.Objects;

/**
 * 向 OAuth 客户端返回当前访问令牌对应的身份声明。
 *
 * <p>Returns identity claims associated with the current access token to OAuth clients.</p>
 */
@RestController
@Tag(name = "oauth-userinfo", description = "统一身份本人信息接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity-profile",
        entityDomainName = "统一身份本人信息域",
        interfaceGroupCode = "tianquan-shoubing-profile"
)

public class OAuthUserInfoController {

    @GetMapping("/oauth2/userinfo")
    @Operation(
            operationId = "tianquan-shoubing-oauth-userinfo-v1",
            summary = "查询 OAuth 当前身份声明",
            tags = {"tianquan-shoubing", "oauth"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public OAuthUserInfoVO userInfo(
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
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
