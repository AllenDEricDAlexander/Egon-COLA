package top.egon.cola.component.gateway.admin.interfaces.management;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.scope.GatewayScopeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gateway/admin/scopes")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
public class GatewayScopeController {

    private final GatewayScopeService service;

    public GatewayScopeController(GatewayScopeService service) {
        this.service = service;
    }

    @GetMapping
    public List<GatewayScopeService.ScopeView> list() {
        return service.list();
    }
}
