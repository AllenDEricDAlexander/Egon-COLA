package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.resource.application.ApplicationResourceFacade;
import top.egon.cola.platform.rbac3.admin.resource.application.ManifestFacade;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;

@RestController
@RequestMapping("/api/rbac3/v1")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "resource-manifest",
        name = "资源清单接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class ManifestController {

    private final ManifestFacade manifestFacade;
    private final ApplicationResourceFacade resourceFacade;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;

    public ManifestController(
            ManifestFacade manifestFacade,
            ApplicationResourceFacade resourceFacade,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock) {
        this.manifestFacade = manifestFacade;
        this.resourceFacade = resourceFacade;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
    }

    @PostMapping("/internal/resource-manifests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RequiresRbac3Permission(permission = "system:resource-manifest:submit")
    @GatewayOperation(
            name = "rbac3-resource-manifest-submit-v1",
            summary = "提交不可变资源清单",
            externalAccessible = false,
            tags = {"rbac3", "resource-manifest"})
    public ApiEnvelope<ManifestFacade.SubmissionResult> submit(
            @Valid @RequestBody SubmitManifestRequest request) {
        return ApiEnvelope.success(manifestFacade.submit(new ManifestFacade.SubmitCommand(
                tenantId(),
                request.applicationId(),
                Long.toString(idGenerator.nextLongId()),
                request.definitionSetId(),
                request.manifest())));
    }

    @GetMapping("/resource-manifests/{manifestId}")
    @RequiresRbac3Permission(permission = "system:resource-manifest:read")
    @GatewayOperation(
            name = "rbac3-resource-manifest-get-v1",
            summary = "查询资源清单",
            externalAccessible = true,
            tags = {"rbac3", "resource-manifest"})
    public ApiEnvelope<ApplicationResourceFacade.ManifestView> manifest(
            @PathVariable String manifestId) {
        return ApiEnvelope.success(resourceFacade.manifest(tenantId(), manifestId));
    }

    @GetMapping("/resource-manifests/{manifestId}/validation")
    @RequiresRbac3Permission(permission = "system:resource-manifest:read")
    @GatewayOperation(
            name = "rbac3-resource-manifest-validation-v1",
            summary = "查询资源清单验证结果",
            externalAccessible = true,
            tags = {"rbac3", "resource-manifest"})
    public ApiEnvelope<ApplicationResourceFacade.ManifestValidationView> validation(
            @PathVariable String manifestId) {
        return ApiEnvelope.success(resourceFacade.validation(tenantId(), manifestId));
    }

    @PostMapping("/resource-manifests/{manifestId}/impact-analysis")
    @RequiresRbac3Permission(permission = "system:resource-manifest:read")
    @GatewayOperation(
            name = "rbac3-resource-manifest-impact-v1",
            summary = "分析资源清单激活影响",
            externalAccessible = true,
            tags = {"rbac3", "resource-manifest"})
    public ApiEnvelope<ApplicationResourceFacade.ManifestImpactView> impact(
            @PathVariable String manifestId) {
        return ApiEnvelope.success(resourceFacade.impact(tenantId(), manifestId));
    }

    @PostMapping("/resource-manifests/{manifestId}/activate")
    @RequiresRbac3Permission(permission = "system:resource-manifest:activate")
    @GatewayOperation(
            name = "rbac3-resource-manifest-activate-v1",
            summary = "原子激活资源清单",
            externalAccessible = true,
            tags = {"rbac3", "resource-manifest"})
    public ApiEnvelope<ManifestFacade.ActivationResult> activate(
            @PathVariable String manifestId,
            @RequestHeader("If-Match") long expectedApplicationVersion,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ActivateManifestRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(manifestFacade.activate(new ManifestFacade.ActivateCommand(
                tenantId(),
                request.applicationId(),
                manifestId,
                expectedApplicationVersion,
                request.expectedCurrentManifestVersion(),
                request.expectedDefinitionSetId(),
                principal.userId(),
                idempotencyKey,
                request.reason()),
                databaseClock.transactionNow()));
    }

    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    public record SubmitManifestRequest(
            @NotBlank String applicationId,
            @NotBlank String definitionSetId,
            @NotNull ResourceManifest manifest) {
    }

    public record ActivateManifestRequest(
            @NotBlank String applicationId,
            @PositiveOrZero long expectedCurrentManifestVersion,
            @NotBlank String expectedDefinitionSetId,
            @NotBlank String reason) {
    }
}
