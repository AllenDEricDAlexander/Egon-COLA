package top.egon.cola.archetype.source.light.adapter.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContext;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder;
import top.egon.cola.archetype.source.light.adapter.user.pojo.convertor.UserAdapterConvertor;
import top.egon.cola.archetype.source.light.adapter.user.pojo.dto.AssignRoleRequest;
import top.egon.cola.archetype.source.light.adapter.user.pojo.vo.UserDetailVO;
import top.egon.cola.archetype.source.light.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.light.application.user.pojo.command.AssignRoleCommand;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties;
import top.egon.cola.platform.tianquan.jianshen.starter.field.FieldWriteAuthorizationCommand;
import top.egon.cola.platform.tianquan.jianshen.starter.field.Rbac3FieldWriteAuthorizationComponent;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RBACAPIResource;

@RestController("roleController")
@RequestMapping("/api/users/{userId}/roles")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "nativeBearer")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Authentication required", headers = {@Header(name = "WWW-Authenticate", required = true, schema = @Schema(type = "string", allowableValues = {"Bearer"})), @Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))}, content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError"))),
        @ApiResponse(responseCode = "403", description = "Authorization denied", headers = {@Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))}, content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError"))),
        @ApiResponse(responseCode = "503", description = "Authorization unavailable", headers = {@Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))}, content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError")))
})
public class RoleController {
    @Qualifier("roleManageImpl")
    private final RoleManage roleManage;
    @Qualifier("userAdapterConvertorImpl")
    private final UserAdapterConvertor convertor;

    @Qualifier("rbac3FieldWriteAuthorizationComponent")
    private final Rbac3FieldWriteAuthorizationComponent fieldWrite;
    @Qualifier("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties")
    private final Rbac3StarterProperties authorizationProperties;

    @PostMapping
    @Operation(operationId = "nativeLightRoleAssignRoleV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-light:role:assign')")
    @RBACAPIResource(code = "egon-cola-source-light.api.role.assign", permission = "egon-cola-source-light:role:assign", name = "Assign role")
    public UserDetailVO assignRole(@PathVariable String userId, @Valid @RequestBody AssignRoleRequest request) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-light:role:assign", authorizationProperties.getSystemCode(),
                "egon-cola-source-light.api.role.assign", Set.of("roleCode")));
        RequestContext context = RequestContextHolder.getRequired();
        return convertor.toTarget(roleManage.assignRole(new AssignRoleCommand(
                Long.valueOf(userId),
                request.roleCode(),
                context.operatorId(),
                context.requestId())));
    }
}
