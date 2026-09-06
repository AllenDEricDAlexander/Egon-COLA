package top.egon.cola.platform.idp.admin.audit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.idp.admin.audit.domain.dto.IdentityAuditQueryDTO;
import top.egon.cola.platform.idp.admin.audit.domain.vo.IdentityAuditPageVO;
import top.egon.cola.platform.idp.admin.audit.service.IdentityAuditService;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.Objects;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/identity/audits")
@Tag(name = "identity-audits", description = "统一身份审计接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity-audit",
        entityDomainName = "统一身份审计域",
        interfaceGroupCode = "idp-audit"
)

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
    @Operation(
            operationId = "idp-identity-audit-list-v1",
            summary = "分页查询统一身份安全审计",
            tags = {"idp", "audit"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public IdentityAuditPageVO list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            @RequestParam(name = "actorSub", required = false) String actorSub,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "result", required = false) String result,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(name = "traceId", required = false) String traceId,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
        ) {
        authorization.require(principal, "idp:audit:read");
        return audits.list(new IdentityAuditQueryDTO(page, size, actorSub, eventType, result, from, to, traceId));
    }
}
