package top.egon.cola.platform.idp.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.identity.application.IdentityUserAdminService;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/identity/users")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份实体域",
        code = "identity-users",
        name = "统一身份用户接口组")
@EgonHttpService(
        serviceName = "idp-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/v1/identity")
public class IdentityUserController {

    private final IdentityUserAdminService users;
    private final IdpAdminAuthorizationPort authorization;

    public IdentityUserController(
            IdentityUserAdminService users,
            IdpAdminAuthorizationPort authorization
    ) {
        this.users = Objects.requireNonNull(users, "users");
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
    }

    @GetMapping
    @GatewayOperation(
            name = "idp-identity-user-list-v1",
            summary = "查询全局身份用户",
            externalAccessible = true,
            tags = {"idp", "identity"})
    public List<IdentityUserAdminService.UserView> list(
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:identity-user:read");
        return users.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @GatewayOperation(
            name = "idp-identity-user-create-v1",
            summary = "创建全局身份用户",
            externalAccessible = true,
            tags = {"idp", "identity"})
    public IdentityUserAdminService.CreatedUserView create(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:identity-user:create");
        return users.create(new IdentityUserAdminService.CreateUserCommand(
                request.username(),
                request.displayName()
        ));
    }

    @PatchMapping("/{subject}")
    @GatewayOperation(
            name = "idp-identity-user-update-v1",
            summary = "更新全局身份用户",
            externalAccessible = true,
            tags = {"idp", "identity"})
    public IdentityUserAdminService.UserView update(
            @PathVariable("subject") String subject,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:identity-user:update");
        return users.update(subject, new IdentityUserAdminService.UpdateUserCommand(
                request.displayName(),
                request.status(),
                request.expectedVersion()
        ));
    }

    @PostMapping("/{subject}/password-reset")
    @GatewayOperation(
            name = "idp-identity-user-password-reset-v1",
            summary = "重置身份用户密码",
            externalAccessible = true,
            tags = {"idp", "identity"})
    public IdentityUserAdminService.ResetPasswordView resetPassword(
            @PathVariable("subject") String subject,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:identity-user:password-reset");
        return users.resetPassword(subject);
    }

    @PostMapping("/{subject}/revoke-all")
    @GatewayOperation(
            name = "idp-identity-user-revoke-all-v1",
            summary = "撤销身份用户全部会话",
            externalAccessible = true,
            tags = {"idp", "identity"})
    public IdentityUserAdminService.UserView revokeAll(
            @PathVariable("subject") String subject,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:identity-user:revoke-all");
        return users.revokeAll(subject);
    }

    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank String displayName
    ) {
    }

    public record UpdateUserRequest(
            @NotBlank String displayName,
            @NotNull IdentityUserStatus status,
            @PositiveOrZero long expectedVersion
    ) {
    }
}
