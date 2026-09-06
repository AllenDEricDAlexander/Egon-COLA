package top.egon.cola.platform.idp.admin.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.Objects;

@RestController
@Tag(name = "identity-profile-me", description = "统一身份本人信息接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity-profile",
        entityDomainName = "统一身份本人信息域",
        interfaceGroupCode = "idp"
)

public class IdentityProfileController {

    private final IdpAdminAuthorizationPort authorization;

    public IdentityProfileController(
            IdpAdminAuthorizationPort authorization
    ) {
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
    }

    @GetMapping("/api/v1/identity/me")
    @Operation(
            operationId = "idp-identity-me-v1",
            summary = "查询当前统一身份",
            tags = {"idp", "identity"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public IdentityPrincipal me(
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:identity:self:read");
        return Objects.requireNonNull(principal, "principal");
    }

}
