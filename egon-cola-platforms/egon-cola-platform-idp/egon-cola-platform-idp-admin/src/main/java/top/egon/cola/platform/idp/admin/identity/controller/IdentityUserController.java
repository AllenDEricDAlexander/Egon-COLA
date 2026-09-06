package top.egon.cola.platform.idp.admin.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.idp.admin.identity.domain.dto.CreateIdentityUserDTO;
import top.egon.cola.platform.idp.admin.identity.domain.dto.UpdateIdentityUserDTO;
import top.egon.cola.platform.idp.admin.identity.domain.vo.CreatedIdentityUserVO;
import top.egon.cola.platform.idp.admin.identity.domain.vo.IdentityUserVO;
import top.egon.cola.platform.idp.admin.identity.domain.vo.ResetPasswordVO;
import top.egon.cola.platform.idp.admin.identity.service.IdentityUserService;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/identity/users")
@Tag(name = "identity-users", description = "统一身份用户接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份实体域",
        interfaceGroupCode = "idp"
)

public class IdentityUserController {

    private final IdentityUserService users;
    private final IdpAdminAuthorizationPort authorization;

    public IdentityUserController(
            IdentityUserService users,
            IdpAdminAuthorizationPort authorization
    ) {
        this.users = Objects.requireNonNull(users, "users");
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
    }

    @GetMapping
    @Operation(
            operationId = "idp-identity-user-list-v1",
            summary = "查询全局身份用户",
            tags = {"idp", "identity"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public List<IdentityUserVO> list(
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:identity-user:read");
        return users.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "idp-identity-user-create-v1",
            summary = "创建全局身份用户",
            tags = {"idp", "identity"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public CreatedIdentityUserVO create(
            @Valid @RequestBody CreateIdentityUserDTO request,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:identity-user:create");
        return users.create(request);
    }

    @PatchMapping("/{subject}")
    @Operation(
            operationId = "idp-identity-user-update-v1",
            summary = "更新全局身份用户",
            tags = {"idp", "identity"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public IdentityUserVO update(
            @PathVariable("subject") String subject,
            @Valid @RequestBody UpdateIdentityUserDTO request,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:identity-user:update");
        return users.update(subject, request);
    }

    @PostMapping("/{subject}/password-reset")
    @Operation(
            operationId = "idp-identity-user-password-reset-v1",
            summary = "重置身份用户密码",
            tags = {"idp", "identity"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResetPasswordVO resetPassword(
            @PathVariable("subject") String subject,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:identity-user:password-reset");
        return users.resetPassword(subject);
    }

    @PostMapping("/{subject}/revoke-all")
    @Operation(
            operationId = "idp-identity-user-revoke-all-v1",
            summary = "撤销身份用户全部会话",
            tags = {"idp", "identity"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public IdentityUserVO revokeAll(
            @PathVariable("subject") String subject,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:identity-user:revoke-all");
        return users.revokeAll(subject);
    }
}
