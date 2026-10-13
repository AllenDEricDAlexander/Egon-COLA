package top.egon.cola.archetype.source.light.adapter.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContext;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder;
import top.egon.cola.archetype.source.light.adapter.user.pojo.convertor.UserAdapterConvertor;
import top.egon.cola.archetype.source.light.adapter.user.pojo.dto.GrantPermissionRequest;
import top.egon.cola.archetype.source.light.adapter.user.pojo.vo.PermissionTreeVO;
import top.egon.cola.archetype.source.light.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.light.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.light.application.user.pojo.query.GetUserPermissionsQuery;
import top.egon.cola.archetype.source.light.application.user.pojo.result.PermissionResult;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties;
import top.egon.cola.platform.tianquan.jianshen.starter.field.FieldWriteAuthorizationCommand;
import top.egon.cola.platform.tianquan.jianshen.starter.field.Rbac3FieldWriteAuthorizationComponent;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RBACAPIResource;

@RestController("permissionController")
@RequestMapping("/api")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
@SecurityRequirement(name = "nativeBearer")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Authentication required", headers = {@Header(name = "WWW-Authenticate", required = true, schema = @Schema(type = "string", allowableValues = {"Bearer"})), @Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))}, content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError"))),
        @ApiResponse(responseCode = "403", description = "Authorization denied", headers = {@Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))}, content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError"))),
        @ApiResponse(responseCode = "503", description = "Authorization unavailable", headers = {@Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))}, content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError")))
})
public class PermissionController {
    @Qualifier("permissionManageImpl")
    private final PermissionManage permissionManage;
    @Qualifier("userAdapterConvertorImpl")
    private final UserAdapterConvertor convertor;

    @Qualifier("rbac3FieldWriteAuthorizationComponent")
    private final Rbac3FieldWriteAuthorizationComponent fieldWrite;
    @Qualifier("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties")
    private final Rbac3StarterProperties authorizationProperties;

    @PostMapping("/roles/{roleCode}/permissions")
    @Operation(operationId = "nativeLightPermissionGrantPermissionV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-light:permission:grant')")
    @RBACAPIResource(code = "egon-cola-source-light.api.permission.grant", permission = "egon-cola-source-light:permission:grant", name = "Grant permission")
    public PermissionResult grantPermission(
            @PathVariable String roleCode,
            @Valid @RequestBody GrantPermissionRequest request) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-light:permission:grant", authorizationProperties.getSystemCode(),
                "egon-cola-source-light.api.permission.grant", Set.of("permissionCode")));
        RequestContext context = RequestContextHolder.getRequired();
        return permissionManage.grantPermission(new GrantPermissionCommand(
                roleCode,
                request.permissionCode(),
                context.operatorId(),
                context.requestId()));
    }

    @GetMapping("/users/{userId}/permissions")
    @Operation(operationId = "nativeLightPermissionGetUserPermissionsV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-light:permission:read')")
    @RBACAPIResource(code = "egon-cola-source-light.api.permission.read", permission = "egon-cola-source-light:permission:read", name = "Read user permissions")
    public List<PermissionTreeVO> getUserPermissions(@PathVariable String userId) {
        return convertor.toPermissionTree(
                permissionManage.getByUser(new GetUserPermissionsQuery(Long.valueOf(userId))));
    }
}
