package top.egon.cola.archetype.source.web.adapter.user.controller;

import top.egon.cola.archetype.source.web.adapter.user.converter.PermissionAdapterConverter;
import top.egon.cola.archetype.source.web.adapter.user.dto.GrantPermissionRequest;
import top.egon.cola.archetype.source.web.adapter.user.vo.PermissionTreeVO;
import top.egon.cola.archetype.source.web.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.web.application.user.query.PermissionTreeQuery;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController("permissionController")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionManage permissionManage;
    private final PermissionAdapterConverter converter;

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
        return converter.toVO(permissionManage.getPermissionTree(new PermissionTreeQuery(
                OrganizationFacadeSupport.positiveId(userId, "userId"))));
    }
}
