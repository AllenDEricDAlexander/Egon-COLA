package top.egon.cola.archetype.source.web.adapter.user.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Set;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties;
import top.egon.cola.platform.tianquan.jianshen.starter.field.FieldWriteAuthorizationCommand;
import top.egon.cola.platform.tianquan.jianshen.starter.field.Rbac3FieldWriteAuthorizationComponent;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RBACAPIResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.PermissionAdapterConverter;
import top.egon.cola.archetype.source.web.adapter.user.pojo.dto.GrantPermissionDTO;
import top.egon.cola.archetype.source.web.adapter.user.pojo.vo.PermissionTreeVO;
import top.egon.cola.archetype.source.web.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.web.application.user.pojo.query.PermissionTreeQuery;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;

@RestController("permissionController")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
@SecurityRequirement(name = "nativeBearer")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Authentication required", headers = {
                @Header(name = "WWW-Authenticate", required = true, schema = @Schema(type = "string", allowableValues = {"Bearer"})),
                @Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))},
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError"))),
        @ApiResponse(responseCode = "403", description = "Authorization denied", headers = {
                @Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))},
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError"))),
        @ApiResponse(responseCode = "503", description = "Authorization unavailable", headers = {
                @Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))},
                content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError")))
})
public class PermissionController {
    @Qualifier("rbac3FieldWriteAuthorizationComponent")
    private final Rbac3FieldWriteAuthorizationComponent fieldWrite;
    @Qualifier("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties")
    private final Rbac3StarterProperties authorizationProperties;


    @Qualifier("permissionManage")
    private final PermissionManage permissionManage;
    @Qualifier("permissionAdapterConverterImpl")
    private final PermissionAdapterConverter converter;

    @PostMapping("/api/v1/roles/{roleCode}/permissions")
    @Operation(operationId = "nativeWebPermissionGrantV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:permission:grant')")
    @RBACAPIResource(code = "egon-cola-source-web.api.permission.grant", permission = "egon-cola-source-web:permission:grant", name = "Grant")
    public ResponseEntity<Void> grant(
            @PathVariable String roleCode,
            @Valid @RequestBody GrantPermissionDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-web:permission:grant", authorizationProperties.getSystemCode(),
                "egon-cola-source-web.api.permission.grant", Set.of("permissionCode")));
        String requestId = key == null ? UUID.randomUUID().toString() : key;
        permissionManage.grantPermission(converter.toCommand(requestId, roleCode, request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/users/{userId}/permissions")
    @Operation(operationId = "nativeWebPermissionGetPermissionTreeV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:permission:read')")
    @RBACAPIResource(code = "egon-cola-source-web.api.permission.read", permission = "egon-cola-source-web:permission:read", name = "Get permission tree")
    public PermissionTreeVO getPermissionTree(@PathVariable String userId) {
        return converter.toVO(permissionManage.getPermissionTree(new PermissionTreeQuery(
                OrganizationFacadeSupport.positiveId(userId, "userId"))));
    }
}
