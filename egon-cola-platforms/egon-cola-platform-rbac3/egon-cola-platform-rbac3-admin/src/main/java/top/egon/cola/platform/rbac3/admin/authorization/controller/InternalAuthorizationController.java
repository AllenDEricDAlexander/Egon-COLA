package top.egon.cola.platform.rbac3.admin.authorization.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.AccessTokenVerification;
import top.egon.cola.platform.idp.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.AuthorizationFenceRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.DecisionRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.ResourceAccessDecisionRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.ResourceAccessRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.DecisionBundleVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.FenceVerificationVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.ResourceAccessDecisionResponseVO;
import top.egon.cola.platform.rbac3.admin.authorization.service.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.runtime.service.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;

import java.util.Objects;

/**
 * Internal RBAC authorization endpoints using a service token plus a verified USER token.
 */
@RestController
@RequestMapping("/internal/v1/authorization")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "internal-authorization",
        name = "内部授权决策接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/internal/v1")
public class InternalAuthorizationController {

    private static final String SUBJECT_TOKEN_HEADER = "X-Egon-Subject-Token";

    private final AuthorizationDecisionService service;
    private final SystemAuthorizationSnapshotService systemSnapshots;
    private final UserAccessTokenVerifier userTokenVerifier;

    public InternalAuthorizationController(
            AuthorizationDecisionService service,
            SystemAuthorizationSnapshotService systemSnapshots,
            UserAccessTokenVerifier userTokenVerifier) {
        this.service = Objects.requireNonNull(service, "service");
        this.systemSnapshots = Objects.requireNonNull(systemSnapshots, "systemSnapshots");
        this.userTokenVerifier = Objects.requireNonNull(userTokenVerifier, "userTokenVerifier");
    }

    /**
     * Loads the current system snapshot for the verified USER subject.
     */
    @GetMapping("/snapshots/current")
    @top.egon.cola.platform.idp.starter.security.RequiresServiceScope("service:authorization:snapshot")
    @GatewayOperation(name = "rbac3-internal-system-snapshot-v2",
            summary = "按已验证用户身份读取系统授权快照",
            externalAccessible = false, tags = {"rbac3", "internal", "authorization"})
    public ApiEnvelopeVO<SystemAuthorizationSnapshot> currentSnapshot(
            @RequestParam String systemCode,
            @RequestHeader(SUBJECT_TOKEN_HEADER) String userAccessToken,
            @AuthenticationPrincipal ServiceIdentityPrincipal servicePrincipal) {
        IdentityPrincipal user = verifyUser(userAccessToken, servicePrincipal.tenantId());
        return ApiEnvelopeVO.success(systemSnapshots.snapshot(
                servicePrincipal.tenantId(), user.subject(), systemCode));
    }

    /** Executes a typed decision after binding the request to the verified USER subject. */
    @PostMapping("/decisions")
    @top.egon.cola.platform.idp.starter.security.RequiresServiceScope("service:authorization:decide")
    @GatewayOperation(name = "rbac3-internal-authorization-decision-v2",
            summary = "使用已验证用户身份执行类型化授权决策",
            externalAccessible = false, tags = {"rbac3", "internal", "authorization"})
    public ApiEnvelopeVO<DecisionBundleVO> decide(
            @Valid @RequestBody DecisionRequestDTO request,
            @RequestHeader(SUBJECT_TOKEN_HEADER) String userAccessToken,
            @AuthenticationPrincipal ServiceIdentityPrincipal servicePrincipal) {
        IdentityPrincipal user = verifyUser(userAccessToken, servicePrincipal.tenantId());
        requireSubject(user, request.subject().tenantId(), request.subject().identitySub());
        return ApiEnvelopeVO.success(service.decide(servicePrincipal, request));
    }

    /** Decides whether the verified USER may enter an application. */
    @PostMapping("/resource-access-decisions")
    @top.egon.cola.platform.idp.starter.security.RequiresServiceScope("service:authorization:decide")
    @GatewayOperation(name = "rbac3-internal-resource-access-decision-v2",
            summary = "判定已验证用户是否具备应用入口权限",
            externalAccessible = false, tags = {"rbac3", "internal", "authorization"})
    public ApiEnvelopeVO<ResourceAccessDecisionResponseVO> decideResourceAccess(
            @Valid @RequestBody ResourceAccessDecisionRequestDTO request,
            @RequestHeader(SUBJECT_TOKEN_HEADER) String userAccessToken,
            @AuthenticationPrincipal ServiceIdentityPrincipal servicePrincipal) {
        IdentityPrincipal user = verifyUser(userAccessToken, servicePrincipal.tenantId());
        requireSubject(user, request.tenantId(), request.identitySub());
        ResourceAccessRequestDTO command = new ResourceAccessRequestDTO(
                user.subject(), servicePrincipal.tenantId(),
                request.rbacApplicationCode(), request.entryPermissionCode());
        return ApiEnvelopeVO.success(ResourceAccessDecisionResponseVO.from(
                service.decideResourceAccess(servicePrincipal, command)));
    }

    /** Checks the user-scoped authorization publication fence. */
    @PostMapping("/fences/verify")
    @top.egon.cola.platform.idp.starter.security.RequiresServiceScope("service:authorization:fence")
    @GatewayOperation(name = "rbac3-internal-authorization-fence-verify-v2",
            summary = "校验用户授权传播 Fence",
            externalAccessible = false, tags = {"rbac3", "internal", "authorization"})
    public ApiEnvelopeVO<FenceVerificationVO> verifyFence(
            @Valid @RequestBody AuthorizationFenceRequestDTO request,
            @RequestHeader(SUBJECT_TOKEN_HEADER) String userAccessToken,
            @AuthenticationPrincipal ServiceIdentityPrincipal servicePrincipal) {
        IdentityPrincipal user = verifyUser(userAccessToken, servicePrincipal.tenantId());
        if (!user.subject().equals(request.identitySub())) {
            throw new Rbac3RuleViolation("IDENTITY_SUBJECT_MISMATCH");
        }
        return ApiEnvelopeVO.success(service.verifyFence(
                servicePrincipal, servicePrincipal.tenantId(), user.subject()));
    }

    private IdentityPrincipal verifyUser(String rawToken, String expectedTenant) {
        AccessTokenVerification<IdentityPrincipal> result = userTokenVerifier.verify(rawToken);
        if (result instanceof AccessTokenVerification.Valid<?> valid
                && valid.principal() instanceof IdentityPrincipal user) {
            if (!expectedTenant.equals(user.tenantId())) {
                throw new Rbac3RuleViolation("TENANT_IDENTITY_MISMATCH");
            }
            return user;
        }
        if (result instanceof AccessTokenVerification.Expired<?>) {
            throw new Rbac3RuleViolation("JWT_EXPIRED");
        }
        if (result instanceof AccessTokenVerification.Invalid<?> invalid) {
            throw new Rbac3RuleViolation(invalid.reasonCode());
        }
        throw new Rbac3RuleViolation("JWT_INVALID");
    }

    private static void requireSubject(
            IdentityPrincipal user,
            String tenantId,
            String identitySub) {
        if (!user.tenantId().equals(tenantId) || !user.subject().equals(identitySub)) {
            throw new Rbac3RuleViolation("IDENTITY_SUBJECT_MISMATCH");
        }
    }
}
