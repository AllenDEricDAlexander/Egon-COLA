package top.egon.cola.platform.idp.admin.token.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.admin.token.domain.dto.PublishSigningKeyDTO;
import top.egon.cola.platform.idp.admin.token.domain.vo.SigningKeyVO;
import top.egon.cola.platform.idp.admin.token.service.SigningKeyService;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/identity/signing-keys")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份实体域",
        code = "signing-keys",
        name = "签名密钥接口组")
@EgonHttpService(
        serviceName = "idp-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/v1/identity")
public class SigningKeyController {

    private final SigningKeyService keys;
    private final IdpAdminAuthorizationPort authorization;

    public SigningKeyController(
            SigningKeyService keys,
            IdpAdminAuthorizationPort authorization
    ) {
        this.keys = Objects.requireNonNull(keys, "keys");
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
    }

    @GetMapping
    @GatewayOperation(name = "idp-signing-key-list-v1",
            summary = "查询签名密钥", externalAccessible = true,
            tags = {"idp", "signing-key"})
    public List<SigningKeyVO> list(
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:signing-key:read");
        return keys.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @GatewayOperation(name = "idp-signing-key-publish-v1",
            summary = "预发布签名密钥", externalAccessible = true,
            tags = {"idp", "signing-key"})
    public SigningKeyVO publish(
            @Valid @RequestBody PublishSigningKeyDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:signing-key:publish");
        return keys.publish(request);
    }

    @PostMapping("/{kid}/activate")
    @GatewayOperation(name = "idp-signing-key-activate-v1",
            summary = "激活签名密钥", externalAccessible = true,
            tags = {"idp", "signing-key"})
    public SigningKeyVO activate(
            @PathVariable("kid") String kid,
            @RequestParam("expectedVersion")
            @PositiveOrZero long expectedVersion,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:signing-key:activate");
        return keys.activate(kid, expectedVersion);
    }

    @PostMapping("/{kid}/retire")
    @GatewayOperation(name = "idp-signing-key-retire-v1",
            summary = "退役签名密钥", externalAccessible = true,
            tags = {"idp", "signing-key"})
    public SigningKeyVO retire(
            @PathVariable("kid") String kid,
            @RequestParam("expectedVersion")
            @PositiveOrZero long expectedVersion,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:signing-key:retire");
        return keys.retire(kid, expectedVersion);
    }
}
