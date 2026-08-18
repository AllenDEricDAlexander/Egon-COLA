package top.egon.cola.platform.idp.admin.identity.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import top.egon.cola.platform.idp.admin.identity.domain.dto.CreateIdentityUserDTO;
import top.egon.cola.platform.idp.admin.identity.domain.dto.UpdateIdentityUserDTO;
import top.egon.cola.platform.idp.admin.identity.domain.vo.CreatedIdentityUserVO;
import top.egon.cola.platform.idp.admin.identity.domain.vo.IdentityUserVO;
import top.egon.cola.platform.idp.admin.identity.domain.vo.ResetPasswordVO;
import top.egon.cola.platform.idp.admin.identity.service.IdentityUserService;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.starter.security.CurrentIdentity;

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

    private final IdentityUserService users;
    private final IdpAdminAuthorizationPort authorization;
    private final CurrentIdentity currentIdentity;

    public IdentityUserController(
            IdentityUserService users,
            IdpAdminAuthorizationPort authorization,
            CurrentIdentity currentIdentity
    ) {
        this.users = Objects.requireNonNull(users, "users");
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
        this.currentIdentity = Objects.requireNonNull(currentIdentity, "currentIdentity");
    }

    @GetMapping
    @GatewayOperation(
            name = "idp-identity-user-list-v1",
            summary = "查询全局身份用户",
            externalAccessible = true,
            tags = {"idp", "identity"})
    public List<IdentityUserVO> list() {
        authorization.require(currentIdentity.require(), "idp:identity-user:read");
        return users.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @GatewayOperation(
            name = "idp-identity-user-create-v1",
            summary = "创建全局身份用户",
            externalAccessible = true,
            tags = {"idp", "identity"})
    public CreatedIdentityUserVO create(
            @Valid @RequestBody CreateIdentityUserDTO request
    ) {
        authorization.require(currentIdentity.require(), "idp:identity-user:create");
        return users.create(request);
    }

    @PatchMapping("/{subject}")
    @GatewayOperation(
            name = "idp-identity-user-update-v1",
            summary = "更新全局身份用户",
            externalAccessible = true,
            tags = {"idp", "identity"})
    public IdentityUserVO update(
            @PathVariable("subject") String subject,
            @Valid @RequestBody UpdateIdentityUserDTO request
    ) {
        authorization.require(currentIdentity.require(), "idp:identity-user:update");
        return users.update(subject, request);
    }

    @PostMapping("/{subject}/password-reset")
    @GatewayOperation(
            name = "idp-identity-user-password-reset-v1",
            summary = "重置身份用户密码",
            externalAccessible = true,
            tags = {"idp", "identity"})
    public ResetPasswordVO resetPassword(
            @PathVariable("subject") String subject
    ) {
        authorization.require(currentIdentity.require(), "idp:identity-user:password-reset");
        return users.resetPassword(subject);
    }

    @PostMapping("/{subject}/revoke-all")
    @GatewayOperation(
            name = "idp-identity-user-revoke-all-v1",
            summary = "撤销身份用户全部会话",
            externalAccessible = true,
            tags = {"idp", "identity"})
    public IdentityUserVO revokeAll(
            @PathVariable("subject") String subject
    ) {
        authorization.require(currentIdentity.require(), "idp:identity-user:revoke-all");
        return users.revokeAll(subject);
    }
}
