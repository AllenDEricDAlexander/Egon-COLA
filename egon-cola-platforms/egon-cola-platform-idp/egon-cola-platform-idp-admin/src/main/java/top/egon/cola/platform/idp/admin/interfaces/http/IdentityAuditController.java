package top.egon.cola.platform.idp.admin.interfaces.http;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.audit.domain.IdentityAuditLogEntity;
import top.egon.cola.platform.idp.admin.audit.infrastructure.IdentityAuditLogRepository;
import top.egon.cola.platform.idp.admin.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.time.Instant;
import java.util.List;
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

    private static final int MAXIMUM_PAGE_SIZE = 200;

    private final IdentityAuditLogRepository audits;
    private final IdpAdminAuthorizationPort authorization;

    public IdentityAuditController(
            IdentityAuditLogRepository audits,
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
    public AuditPage list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:audit:read");
        if (page < 0 || size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException("invalid audit page request");
        }
        Page<IdentityAuditLogEntity> result = audits.findAll(PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "occurredAt", "id")
        ));
        return new AuditPage(
                result.getContent().stream().map(AuditView::from).toList(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public record AuditPage(
            List<AuditView> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    public record AuditView(
            String id,
            String eventType,
            String actorSub,
            String targetSub,
            String result,
            String reason,
            Instant occurredAt
    ) {

        private static AuditView from(IdentityAuditLogEntity entity) {
            return new AuditView(
                    entity.getId(),
                    entity.getEventType(),
                    entity.getActorSub(),
                    entity.getTargetSub(),
                    entity.getResult(),
                    entity.getReason(),
                    entity.getOccurredAt()
            );
        }
    }
}
