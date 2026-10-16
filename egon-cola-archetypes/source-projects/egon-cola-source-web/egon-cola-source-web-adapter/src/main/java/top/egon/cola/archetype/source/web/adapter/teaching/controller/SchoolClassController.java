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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import top.egon.cola.archetype.source.web.adapter.teaching.pojo.convertor.SchoolClassAdapterConverter;
import top.egon.cola.archetype.source.web.adapter.teaching.pojo.dto.AssignUserToClassDTO;
import top.egon.cola.archetype.source.web.adapter.teaching.pojo.dto.CreateSchoolClassDTO;
import top.egon.cola.archetype.source.web.adapter.teaching.pojo.vo.SchoolClassDetailVO;
import top.egon.cola.archetype.source.web.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.web.application.teaching.pojo.query.SchoolClassDetailQuery;
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

@RestController("schoolClassController")
@RequestMapping("/api/v1")
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
public class SchoolClassController {
    @Qualifier("rbac3FieldWriteAuthorizationComponent")
    private final Rbac3FieldWriteAuthorizationComponent fieldWrite;
    @Qualifier("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties")
    private final Rbac3StarterProperties authorizationProperties;

    @Qualifier("schoolClassManage")
    private final SchoolClassManage schoolClassManage;
    @Qualifier("schoolClassAdapterConverterImpl")
    private final SchoolClassAdapterConverter converter;

    @PostMapping("/school-classes")
    @Operation(operationId = "nativeWebSchoolClassCreateV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:school-class:create')")
    @RBACAPIResource(code = "egon-cola-source-web.api.school-class.create", permission = "egon-cola-source-web:school-class:create", name = "Create")
    public ResponseEntity<SchoolClassDetailVO> create(
            @Valid @RequestBody CreateSchoolClassDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-web:school-class:create", authorizationProperties.getSystemCode(),
                "egon-cola-source-web.api.school-class.create", Set.of("name","gradeCode")));
        String requestId = key == null ? UUID.randomUUID().toString() : key;
        SchoolClassDetailVO result = converter.toVO(
            schoolClassManage.createSchoolClass(converter.toCommand(requestId, request)));
        return ResponseEntity.created(URI.create("/api/v1/school-classes/" + result.id())).body(result);
    }

    @GetMapping("/grades/{gradeId}/school-classes/{schoolClassId}")
    @Operation(operationId = "nativeWebSchoolClassGetV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:school-class:read')")
    @RBACAPIResource(code = "egon-cola-source-web.api.school-class.read", permission = "egon-cola-source-web:school-class:read", name = "Get")
    public SchoolClassDetailVO get(
            @PathVariable String gradeId,
            @PathVariable String schoolClassId) {
        return converter.toVO(
                schoolClassManage.getSchoolClass(new SchoolClassDetailQuery(
                        OrganizationFacadeSupport.positiveId(gradeId, "gradeId"),
                        OrganizationFacadeSupport.positiveId(schoolClassId, "schoolClassId"))));
    }

    @PostMapping("/grades/{gradeId}/school-classes/{schoolClassId}/users")
    @Operation(operationId = "nativeWebSchoolClassAssignUserV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:school-class:assign-user')")
    @RBACAPIResource(code = "egon-cola-source-web.api.school-class.assign-user", permission = "egon-cola-source-web:school-class:assign-user", name = "Assign user")
    public ResponseEntity<Void> assignUser(
            @PathVariable String gradeId,
            @PathVariable String schoolClassId,
            @Valid @RequestBody AssignUserToClassDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-web:school-class:assign-user", authorizationProperties.getSystemCode(),
                "egon-cola-source-web.api.school-class.assign-user", Set.of("userId")));
        String requestId = key == null ? UUID.randomUUID().toString() : key;
        schoolClassManage.assignUser(
                converter.toCommand(requestId,
                        OrganizationFacadeSupport.positiveId(gradeId, "gradeId"),
                        OrganizationFacadeSupport.positiveId(schoolClassId, "schoolClassId"),
                        OrganizationFacadeSupport.positiveId(request.userId(), "userId")));
        return ResponseEntity.noContent().build();
    }
}
