package top.egon.cola.component.gateway.admin.interfaces.openapi;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayDefinitionReportService;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayReportAuthentication;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;

@RestController
@RequestMapping(
        "/api/v1/gateway/openapi/interface-definitions/reports"
)
public class GatewayDefinitionReportController {

    private final GatewayDefinitionReportService reports;

    public GatewayDefinitionReportController(
            GatewayDefinitionReportService reports) {
        this.reports = reports;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GatewayInterfaceDefinitionReportResult report(
            @RequestBody GatewayInterfaceDefinitionReport report,
            @RequestHeader("X-Gateway-Report-Id") String reportId,
            @RequestHeader("X-Gateway-Contract-Version")
            String contractVersion,
            HttpServletRequest request) {
        return reports.accept(
                authentication(request),
                report,
                reportId,
                contractVersion
        );
    }

    @GetMapping("/{reportId}")
    public GatewayInterfaceDefinitionReportResult find(
            @PathVariable String reportId,
            HttpServletRequest request) {
        return reports.find(authentication(request), reportId);
    }

    private GatewayReportAuthentication authentication(
            HttpServletRequest request) {
        Object value = request.getAttribute(
                GatewayReportAuthentication.REQUEST_ATTRIBUTE
        );
        if (value instanceof GatewayReportAuthentication authentication) {
            return authentication;
        }
        throw new IllegalStateException(
                "GATEWAY_REPORT_AUTHENTICATION_REQUIRED"
        );
    }
}
