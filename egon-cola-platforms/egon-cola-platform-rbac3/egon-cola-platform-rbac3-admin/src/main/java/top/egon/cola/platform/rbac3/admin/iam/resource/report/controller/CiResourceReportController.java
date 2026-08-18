package top.egon.cola.platform.rbac3.admin.iam.resource.report.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.dto.CiResourceReportRequestDTO;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.vo.CiResourceReportResultVO;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.service.CiResourceReportService;

import java.util.Objects;

/** CI-only global resource report endpoint; it never accepts a tenant id. */
@RestController
@RequestMapping("/api/rbac3/v1/iam/resource-catalog")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public final class CiResourceReportController {

    private final CiResourceReportService service;

    public CiResourceReportController(CiResourceReportService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @PutMapping("/businesses/{businessCode}/applications/{applicationCode}/frontend-resources")
    @RequiresServiceScope(CiResourceReportService.REPORT_SCOPE)
    @GatewayOperation(
            name = "rbac3-resource-catalog-report-v1",
            summary = "接收流水线前端资源报告",
            externalAccessible = true,
            tags = {"rbac3", "resource", "ci"})
    public CiResourceReportResultVO report(
            @PathVariable String businessCode,
            @PathVariable String applicationCode,
            @AuthenticationPrincipal ServiceIdentityPrincipal principal,
            @Valid @RequestBody CiResourceReportRequestDTO request) {
        return service.report(businessCode, applicationCode, principal, request);
    }
}
