package top.egon.cola.platform.rbac3.admin.registration.ci.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import io.swagger.v3.oas.annotations.Operation;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;
import top.egon.cola.platform.rbac3.admin.registration.ci.domain.dto.CiResourceRegistrationRequestDTO;
import top.egon.cola.platform.rbac3.admin.registration.ci.domain.vo.CiResourceRegistrationResultVO;
import top.egon.cola.platform.rbac3.admin.registration.ci.service.CiResourceRegistrationService;

import java.util.Objects;

/** CI-only global resource registration endpoint; it never accepts a tenant id. */
@RestController
@RequestMapping("/api/rbac3/v1/registration")
public class CiResourceRegistrationController {

    private final CiResourceRegistrationService service;

    public CiResourceRegistrationController(CiResourceRegistrationService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @PutMapping("/businesses/{businessCode}/applications/{applicationCode}/frontend-resources")
    @RequiresServiceScope(CiResourceRegistrationService.REGISTRATION_SCOPE)
    @Operation(
            operationId = "rbac3-resource-registration-v1",
            summary = "接收流水线前端资源注册",
            tags = {"rbac3", "resource", "ci"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<CiResourceRegistrationResultVO> register(
            @PathVariable String businessCode,
            @PathVariable String applicationCode,
            @AuthenticationPrincipal ServiceIdentityPrincipal principal,
            @Valid @RequestBody CiResourceRegistrationRequestDTO request) {
        return ResultRecord.success(service.register(
                businessCode, applicationCode, principal, request));
    }
}
