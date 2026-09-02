package top.egon.cola.archetype.source.web.adapter.user.controller;

import top.egon.cola.archetype.source.web.adapter.user.converter.RoleAdapterConverter;
import top.egon.cola.archetype.source.web.adapter.user.dto.AssignRoleRequest;
import top.egon.cola.archetype.source.web.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController("roleController")
@RequiredArgsConstructor
public class RoleController {

    private final RoleManage roleManage;
    private final RoleAdapterConverter converter;

    @PostMapping("/api/v1/users/{userId}/roles")
    public ResponseEntity<Void> assign(
            @PathVariable String userId,
            @Valid @RequestBody AssignRoleRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        String requestId = key == null ? UUID.randomUUID().toString() : key;
        roleManage.assignRole(converter.toCommand(requestId,
                OrganizationFacadeSupport.positiveId(userId, "userId"), request));
        return ResponseEntity.noContent().build();
    }
}
