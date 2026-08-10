package top.egon.cola.platform.idp.admin.audit.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.audit.domain.dto.IdentityAuditQueryDTO;
import top.egon.cola.platform.idp.admin.audit.domain.vo.IdentityAuditPageVO;
import top.egon.cola.platform.idp.admin.audit.service.IdentityAuditService;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/identity/audits")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity-audit",
        entityDomainName = "统一身份审计域",
        code = "identity-audits",
        name = "统一身份审计接口组")
@EgonHttpService(
        serviceName = "idp-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/v1/identity")
public class IdentityAuditController {

    private final IdentityAuditService audits;
    private final IdpAdminAuthorizationPort authorization;

    public IdentityAuditController(
            IdentityAuditService audits,
            IdpAdminAuthorizationPort authorization
    ) {
        this.audits = Objects.requireNonNull(audits, "audits");
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
    }

    @GetMapping
    @GatewayOperation(
            name = "idp-identity-audit-list-v1",
            summary = "分页查询统一身份安全审计",
            externalAccessible = true,
            tags = {"idp", "audit"})
    public IdentityAuditPageVO list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            @AuthenticationPrincipal IdentityPrincipal principal
        ) {
        authorization.require(principal, "idp:audit:read");
        return audits.list(new IdentityAuditQueryDTO(page, size));
    }
}
