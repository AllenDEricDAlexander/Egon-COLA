package ${package}.adapter.controller.user;

import ${package}.adapter.converter.PermissionAdapterConverter;
import ${package}.adapter.dto.user.GrantPermissionRequest;
import ${package}.adapter.vo.user.PermissionTreeVO;
import ${package}.application.manage.user.PermissionManage;
import ${package}.application.query.user.PermissionTreeQuery;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController("permissionController")
public class PermissionController {

    private final PermissionManage permissionManage;
    private final PermissionAdapterConverter converter;

    public PermissionController(PermissionManage permissionManage, PermissionAdapterConverter converter) {
        this.permissionManage = permissionManage;
        this.converter = converter;
    }

    @PostMapping("/api/v1/roles/{roleCode}/permissions")
    public ResponseEntity<Void> grant(
            @PathVariable String roleCode,
            @Valid @RequestBody GrantPermissionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        String requestId = key == null ? UUID.randomUUID().toString() : key;
        permissionManage.grantPermission(converter.toCommand(requestId, roleCode, request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/users/{userId}/permissions")
    public PermissionTreeVO getPermissionTree(@PathVariable String userId) {
        return converter.toVO(permissionManage.getPermissionTree(new PermissionTreeQuery(userId)));
    }
}
