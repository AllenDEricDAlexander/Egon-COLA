package ${package}.adapter.controller.user;

import ${package}.adapter.converter.RoleAdapterConverter;
import ${package}.adapter.dto.user.AssignRoleRequest;
import ${package}.application.manage.user.RoleManage;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController("roleController")
public class RoleController {

    private final RoleManage roleManage;
    private final RoleAdapterConverter converter;

    public RoleController(RoleManage roleManage, RoleAdapterConverter converter) {
        this.roleManage = roleManage;
        this.converter = converter;
    }

    @PostMapping("/api/v1/users/{userId}/roles")
    public ResponseEntity<Void> assign(
            @PathVariable String userId,
            @Valid @RequestBody AssignRoleRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        String requestId = key == null ? UUID.randomUUID().toString() : key;
        roleManage.assignRole(converter.toCommand(requestId, userId, request));
        return ResponseEntity.noContent().build();
    }
}
