package top.egon.cola.archetype.source.webopen.adapter.user.controller;

import top.egon.cola.archetype.source.webopen.adapter.user.converter.RoleAdapterConverter;
import top.egon.cola.archetype.source.webopen.adapter.user.dto.AssignRoleRequest;
import top.egon.cola.archetype.source.webopen.adapter.facade.impl.OrganizationIdBoundary;
import top.egon.cola.archetype.source.webopen.application.user.manage.RoleManage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import top.egon.cola.component.common.id.generator.LongIdGenerator;

@RestController("roleController")
@RequiredArgsConstructor
public class RoleController {

    private final RoleManage roleManage;
    private final RoleAdapterConverter converter;
    private final LongIdGenerator idGenerator;

    @PostMapping("/api/v1/users/{userId}/roles")
    public ResponseEntity<Void> assign(
            @PathVariable String userId,
            @Valid @RequestBody AssignRoleRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        String requestId = key == null || key.isBlank() ? Long.toString(idGenerator.nextLongId()) : key;
        roleManage.assignRole(converter.toCommand(requestId,
            OrganizationIdBoundary.parse(userId, "userId"), request));
        return ResponseEntity.noContent().build();
    }
}
