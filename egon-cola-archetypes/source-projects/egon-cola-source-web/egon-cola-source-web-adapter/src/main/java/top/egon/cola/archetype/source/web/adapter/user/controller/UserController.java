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
import top.egon.cola.archetype.source.web.adapter.user.pojo.convertor.UserAdapterConverter;
import top.egon.cola.archetype.source.web.adapter.user.pojo.dto.CreateUserDTO;
import top.egon.cola.archetype.source.web.adapter.user.pojo.vo.UserDetailVO;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
import top.egon.cola.archetype.source.web.application.user.pojo.query.UserDetailQuery;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;

@RestController("userController")
@RequestMapping("/api/v1/users")
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
public class UserController {
    @Qualifier("rbac3FieldWriteAuthorizationComponent")
    private final Rbac3FieldWriteAuthorizationComponent fieldWrite;
    @Qualifier("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties")
    private final Rbac3StarterProperties authorizationProperties;


    @Qualifier("userManage")
    private final UserManage userManage;
    @Qualifier("userAdapterConverterImpl")
    private final UserAdapterConverter converter;

    @PostMapping
    @Operation(operationId = "nativeWebUserCreateV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:user:create')")
    @RBACAPIResource(code = "egon-cola-source-web.api.user.create", permission = "egon-cola-source-web:user:create", name = "Create")
    public ResponseEntity<UserDetailVO> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateUserDTO request) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-web:user:create", authorizationProperties.getSystemCode(),
                "egon-cola-source-web.api.user.create", Set.of("name","email")));
        String requestId = idempotencyKey == null ? UUID.randomUUID().toString() : idempotencyKey;
        UserDetailVO body = converter.toVO(userManage.createUser(converter.toCommand(requestId, request)));
        return ResponseEntity.created(URI.create("/api/v1/users/" + body.id())).body(body);
    }

    @GetMapping("/{userId}")
    @Operation(operationId = "nativeWebUserGetV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:user:read')")
    @RBACAPIResource(code = "egon-cola-source-web.api.user.read", permission = "egon-cola-source-web:user:read", name = "Get")
    public UserDetailVO get(@PathVariable String userId) {
        return converter.toVO(userManage.getUser(new UserDetailQuery(
                OrganizationFacadeSupport.positiveId(userId, "userId"))));
    }
}
