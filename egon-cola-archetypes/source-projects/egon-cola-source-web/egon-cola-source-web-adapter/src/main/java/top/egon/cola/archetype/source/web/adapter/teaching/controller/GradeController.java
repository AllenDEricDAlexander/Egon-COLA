package top.egon.cola.archetype.source.web.adapter.teaching.controller;

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
import org.springframework.security.access.prepost.PreAuthorize;
import top.egon.cola.archetype.source.web.adapter.teaching.pojo.convertor.GradeAdapterConverter;
import top.egon.cola.archetype.source.web.adapter.teaching.pojo.dto.CreateGradeRequest;
import top.egon.cola.archetype.source.web.adapter.teaching.pojo.vo.GradeDetailVO;
import top.egon.cola.archetype.source.web.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.web.application.teaching.pojo.query.GradeDetailQuery;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;

@RestController("gradeController")
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
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
public class GradeController {
    @Qualifier("rbac3FieldWriteAuthorizationComponent")
    private final Rbac3FieldWriteAuthorizationComponent fieldWrite;
    @Qualifier("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties")
    private final Rbac3StarterProperties authorizationProperties;

    @Qualifier("gradeManage")
    private final GradeManage gradeManage;
    @Qualifier("gradeAdapterConverterImpl")
    private final GradeAdapterConverter converter;

    @PostMapping
    @Operation(operationId = "nativeWebGradeCreateV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:grade:create')")
    @RBACAPIResource(code = "egon-cola-source-web.api.grade.create", permission = "egon-cola-source-web:grade:create", name = "Create")
    public ResponseEntity<GradeDetailVO> create(
            @Valid @RequestBody CreateGradeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-web:grade:create", authorizationProperties.getSystemCode(),
                "egon-cola-source-web.api.grade.create", Set.of("code","name")));
        String requestId = key == null ? UUID.randomUUID().toString() : key;
        GradeDetailVO result = converter.toVO(gradeManage.createGrade(converter.toCommand(requestId, request)));
        return ResponseEntity.created(URI.create("/api/v1/grades/" + result.id())).body(result);
    }

    @GetMapping("/{gradeId}")
    @Operation(operationId = "nativeWebGradeGetV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:grade:read')")
    @RBACAPIResource(code = "egon-cola-source-web.api.grade.read", permission = "egon-cola-source-web:grade:read", name = "Get")
    public GradeDetailVO get(@PathVariable String gradeId) {
        return converter.toVO(gradeManage.getGrade(new GradeDetailQuery(
                OrganizationFacadeSupport.positiveId(gradeId, "gradeId"))));
    }
}
