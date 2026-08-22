package top.egon.cola.platform.idp.admin.tenant.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.admin.tenant.domain.dto.CreateTenantDTO;
import top.egon.cola.platform.idp.admin.tenant.domain.dto.UpdateTenantDTO;
import top.egon.cola.platform.idp.admin.tenant.domain.dto.UpsertTenantMembershipDTO;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantEntity;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantMembershipEntity;
import top.egon.cola.platform.idp.admin.tenant.domain.vo.TenantMembershipVO;
import top.egon.cola.platform.idp.admin.tenant.domain.vo.TenantVO;
import top.egon.cola.platform.idp.admin.tenant.service.TenantMembershipService;
import top.egon.cola.platform.idp.admin.tenant.service.TenantService;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** HTTP administration surface for the IdP-owned tenant authority. */
@Validated
@RestController
@RequestMapping("/api/v1/identity/tenants")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份实体域",
        code = "identity-tenants",
        name = "统一身份租户接口组")
@EgonHttpService(
        serviceName = "idp-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/v1/identity")
public class TenantController {

    private final TenantService tenants;
    private final TenantMembershipService memberships;
    private final IdpAdminAuthorizationPort authorization;
    private final ObjectMapper objectMapper;

    public TenantController(
            TenantService tenants,
            TenantMembershipService memberships,
            IdpAdminAuthorizationPort authorization,
            ObjectMapper objectMapper
    ) {
        this.tenants = Objects.requireNonNull(tenants, "tenants");
        this.memberships = Objects.requireNonNull(memberships, "memberships");
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @GetMapping
    @GatewayOperation(
            name = "idp-tenant-list-v1",
            summary = "查询身份租户",
            externalAccessible = true,
            tags = {"idp", "tenant"})
    public TenantPageVO list(
            @RequestParam(defaultValue = "0")
            @Min(0) int page,
            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) IdentityTenantEntity.Status status,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:tenant:read");
        validatePage(page, size);
        String normalizedQuery = normalizedQuery(query);
        List<TenantVO> filtered = tenants.list().stream()
                .filter(view -> status == null || view.status() == status)
                .filter(view -> normalizedQuery == null
                        || view.tenantCode().toLowerCase(Locale.ROOT)
                        .contains(normalizedQuery)
                        || view.tenantName().toLowerCase(Locale.ROOT)
                        .contains(normalizedQuery))
                .map(this::tenant)
                .toList();
        return page(filtered, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @GatewayOperation(
            name = "idp-tenant-create-v1",
            summary = "创建身份租户",
            externalAccessible = true,
            tags = {"idp", "tenant"})
    public TenantVO create(
            @Valid @RequestBody CreateTenantDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:tenant:manage");
        return tenant(tenants.create(new TenantService.CreateTenantCommand(
                request.tenantCode(),
                request.tenantName(),
                settings(request.settings()),
                operator(principal)
        )));
    }

    @PatchMapping("/{tenantId}")
    @GatewayOperation(
            name = "idp-tenant-update-v1",
            summary = "更新身份租户",
            externalAccessible = true,
            tags = {"idp", "tenant"})
    public TenantVO update(
            @PathVariable String tenantId,
            @Valid @RequestBody UpdateTenantDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:tenant:manage");
        return tenant(tenants.update(
                tenantId,
                new TenantService.UpdateTenantCommand(
                        request.expectedVersion(),
                        request.tenantName(),
                        settings(request.settings()),
                        request.status(),
                        operator(principal)
                )
        ));
    }

    @GetMapping("/{tenantId}/members")
    @GatewayOperation(
            name = "idp-tenant-membership-list-v1",
            summary = "查询身份租户成员",
            externalAccessible = true,
            tags = {"idp", "tenant", "membership"})
    public TenantMembershipPageVO listMembers(
            @PathVariable String tenantId,
            @RequestParam(defaultValue = "0")
            @Min(0) int page,
            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false)
            IdentityTenantMembershipEntity.Status status,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:tenant:read");
        validatePage(page, size);
        String normalizedQuery = normalizedQuery(query);
        List<TenantMembershipVO> filtered = memberships.listByTenant(tenantId)
                .stream()
                .filter(view -> status == null || view.status() == status)
                .filter(view -> normalizedQuery == null
                        || view.identitySub().toLowerCase(Locale.ROOT)
                        .contains(normalizedQuery)
                        || view.displayName().toLowerCase(Locale.ROOT)
                        .contains(normalizedQuery))
                .map(view -> membership(null, view))
                .toList();
        return pageMembers(filtered, page, size);
    }

    @PutMapping("/{tenantId}/members/{identitySub}")
    @GatewayOperation(
            name = "idp-tenant-membership-upsert-v1",
            summary = "更新身份租户成员",
            externalAccessible = true,
            tags = {"idp", "tenant", "membership"})
    public ResponseEntity<TenantMembershipVO> upsertMember(
            @PathVariable String tenantId,
            @PathVariable String identitySub,
            @Valid @RequestBody UpsertTenantMembershipDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:tenant:manage");
        TenantMembershipService.MembershipView view = memberships.upsert(
                new TenantMembershipService.UpsertMembershipCommand(
                        tenantId,
                        identitySub,
                        request.status(),
                        request.expectedVersion(),
                        operator(principal)
                )
        );
        HttpStatus status = request.expectedVersion() == null
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(membership(view.tenantId(), view));
    }

    private TenantVO tenant(TenantService.TenantView view) {
        return new TenantVO(
                view.tenantId(),
                view.tenantCode(),
                view.tenantName(),
                view.status(),
                readSettings(view.settings()),
                view.version(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    private JsonNode readSettings(String value) {
        try {
            JsonNode node = objectMapper.readTree(value == null ? "{}" : value);
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException("settings must be an object");
            }
            return node;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("settings must be valid JSON", exception);
        }
    }

    private String settings(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("settings must be an object");
        }
        String value;
        try {
            value = objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("settings must be valid JSON", exception);
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > 65_536) {
            throw new IllegalArgumentException("settings is too large");
        }
        return value;
    }

    private static String normalizedQuery(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("query is too long");
        }
        return normalized;
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page or size is invalid");
        }
    }

    private static TenantPageVO page(List<TenantVO> values, int page, int size) {
        int from = Math.min(page * size, values.size());
        int to = Math.min(from + size, values.size());
        int totalPages = values.isEmpty() ? 0 : (values.size() + size - 1) / size;
        return new TenantPageVO(
                values.subList(from, to),
                page,
                size,
                values.size(),
                totalPages
        );
    }

    private static TenantMembershipPageVO pageMembers(
            List<TenantMembershipVO> values,
            int page,
            int size
    ) {
        int from = Math.min(page * size, values.size());
        int to = Math.min(from + size, values.size());
        int totalPages = values.isEmpty() ? 0 : (values.size() + size - 1) / size;
        return new TenantMembershipPageVO(
                values.subList(from, to),
                page,
                size,
                values.size(),
                totalPages
        );
    }

    private static TenantMembershipVO membership(
            String tenantId,
            TenantMembershipService.MembershipView view
    ) {
        return new TenantMembershipVO(
                tenantId,
                view.identitySub(),
                view.displayName(),
                view.status(),
                view.version(),
                view.updatedAt()
        );
    }

    private static String operator(IdentityPrincipal principal) {
        return principal == null ? "SYSTEM" : principal.subject();
    }

    public record TenantPageVO(
            List<TenantVO> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    public record TenantMembershipPageVO(
            List<TenantMembershipVO> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
