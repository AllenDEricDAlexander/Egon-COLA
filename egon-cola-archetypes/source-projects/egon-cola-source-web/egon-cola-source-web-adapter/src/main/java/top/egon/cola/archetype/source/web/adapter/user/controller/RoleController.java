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
import top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.RoleAdapterConverter;
import top.egon.cola.archetype.source.web.adapter.user.pojo.dto.AssignRoleDTO;
import top.egon.cola.archetype.source.web.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;

@RestController("roleController")
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
public class RoleController {
    @Qualifier("rbac3FieldWriteAuthorizationComponent")
    private final Rbac3FieldWriteAuthorizationComponent fieldWrite;
    @Qualifier("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties")
    private final Rbac3StarterProperties authorizationProperties;


    @Qualifier("roleManage")
    private final RoleManage roleManage;
    @Qualifier("roleAdapterConverterImpl")
    private final RoleAdapterConverter converter;

    @PostMapping("/api/v1/users/{userId}/roles")
    @Operation(operationId = "nativeWebRoleAssignV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:role:assign')")
    @RBACAPIResource(code = "egon-cola-source-web.api.role.assign", permission = "egon-cola-source-web:role:assign", name = "Assign")
    public ResponseEntity<Void> assign(
            @PathVariable String userId,
            @Valid @RequestBody AssignRoleDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-web:role:assign", authorizationProperties.getSystemCode(),
                "egon-cola-source-web.api.role.assign", Set.of("roleCode")));
        String requestId = key == null ? UUID.randomUUID().toString() : key;
        roleManage.assignRole(converter.toCommand(requestId,
                OrganizationFacadeSupport.positiveId(userId, "userId"), request));
        return ResponseEntity.noContent().build();
    }
}
