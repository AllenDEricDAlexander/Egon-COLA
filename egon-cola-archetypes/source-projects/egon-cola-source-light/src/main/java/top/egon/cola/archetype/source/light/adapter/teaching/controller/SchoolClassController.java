package top.egon.cola.archetype.source.light.adapter.teaching.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
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
import top.egon.cola.archetype.source.light.adapter.teaching.pojo.convertor.TeachingAdapterConvertor;
import top.egon.cola.archetype.source.light.adapter.teaching.pojo.dto.CreateSchoolClassRequest;
import top.egon.cola.archetype.source.light.adapter.teaching.pojo.dto.ScheduleCourseRequest;
import top.egon.cola.archetype.source.light.adapter.teaching.pojo.vo.SchoolClassDetailVO;
import top.egon.cola.archetype.source.light.adapter.teaching.validators.TeachingRequestValidator;
import top.egon.cola.archetype.source.light.application.teaching.manage.SchoolClassManage;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetSchoolClassQuery;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties;
import top.egon.cola.platform.tianquan.jianshen.starter.field.FieldWriteAuthorizationCommand;
import top.egon.cola.platform.tianquan.jianshen.starter.field.Rbac3FieldWriteAuthorizationComponent;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RBACAPIResource;

@RestController("schoolClassController")
@RequestMapping("/api/school-classes")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
@SecurityRequirement(name = "nativeBearer")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Authentication required", headers = {@Header(name = "WWW-Authenticate", required = true, schema = @Schema(type = "string", allowableValues = {"Bearer"})), @Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))}, content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError"))),
        @ApiResponse(responseCode = "403", description = "Authorization denied", headers = {@Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))}, content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError"))),
        @ApiResponse(responseCode = "503", description = "Authorization unavailable", headers = {@Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))}, content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError")))
})
public class SchoolClassController {
    @Qualifier("schoolClassManageImpl")
    private final SchoolClassManage schoolClassManage;
    @Qualifier("teachingRequestValidator")
    private final TeachingRequestValidator validator;
    @Qualifier("teachingAdapterConvertorImpl")
    private final TeachingAdapterConvertor convertor;

    @Qualifier("rbac3FieldWriteAuthorizationComponent")
    private final Rbac3FieldWriteAuthorizationComponent fieldWrite;
    @Qualifier("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties")
    private final Rbac3StarterProperties authorizationProperties;

    @PostMapping
    @Operation(operationId = "nativeLightSchoolClassCreateV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-light:school-class:create')")
    @RBACAPIResource(code = "egon-cola-source-light.api.school-class.create", permission = "egon-cola-source-light:school-class:create", name = "Create school class")
    public SchoolClassDetailVO create(@Valid @RequestBody CreateSchoolClassRequest request) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-light:school-class:create", authorizationProperties.getSystemCode(),
                "egon-cola-source-light.api.school-class.create", Set.of("name", "semester")));
        RequestContext context = RequestContextHolder.getRequired();
        return convertor.toSchoolClassDetail(schoolClassManage.create(new CreateSchoolClassCommand(
                request.name(), request.semester(), context.operatorId(), context.requestId())));
    }

    @GetMapping("/{schoolClassId}")
    @Operation(operationId = "nativeLightSchoolClassGetV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-light:school-class:read')")
    @RBACAPIResource(code = "egon-cola-source-light.api.school-class.read", permission = "egon-cola-source-light:school-class:read", name = "Read school class")
    public SchoolClassDetailVO get(@PathVariable String schoolClassId) {
        return convertor.toSchoolClassDetail(
                schoolClassManage.get(new GetSchoolClassQuery(Long.valueOf(schoolClassId))));
    }

    @PostMapping("/{schoolClassId}/courses/{courseId}/schedule")
    @Operation(operationId = "nativeLightSchoolClassScheduleV1", description = "Offset or Z datetime input; windows are truncated to microseconds before validation and persistence. Returns the existing ResultRecord envelope with SchoolClassDetailVO in data.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scheduled course summary in the existing success envelope",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "object"), schemaProperties = {
                            @SchemaProperty(name = "success", schema = @Schema(type = "boolean")),
                            @SchemaProperty(name = "code", schema = @Schema(type = "integer", format = "int32")),
                            @SchemaProperty(name = "status", schema = @Schema(type = "string")),
                            @SchemaProperty(name = "message", schema = @Schema(type = "string")),
                            @SchemaProperty(name = "data", schema = @Schema(implementation = SchoolClassDetailVO.class)),
                            @SchemaProperty(name = "traceId", schema = @Schema(type = "string", nullable = true)),
                            @SchemaProperty(name = "timestamp", schema = @Schema(type = "integer", format = "int64", description = "Unix milliseconds"))
                    })),
            @ApiResponse(responseCode = "400", description = "Invalid offset datetime or canonical window; existing validation/business error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResultRecord.class)))
    })
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-light:school-class:schedule-course')")
    @RBACAPIResource(code = "egon-cola-source-light.api.school-class.schedule-course", permission = "egon-cola-source-light:school-class:schedule-course", name = "Schedule course")
    public SchoolClassDetailVO schedule(
            @PathVariable String schoolClassId,
            @PathVariable String courseId,
            @Valid @RequestBody ScheduleCourseRequest request) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-light:school-class:schedule-course", authorizationProperties.getSystemCode(),
                "egon-cola-source-light.api.school-class.schedule-course", Set.of("startsAt", "endsAt")));
        validator.validateSchedule(request);
        RequestContext context = RequestContextHolder.getRequired();
        return convertor.toSchoolClassDetail(schoolClassManage.schedule(new ScheduleCourseCommand(
                Long.valueOf(schoolClassId),
                Long.valueOf(courseId),
                request.startsAt(),
                request.endsAt(),
                context.operatorId(),
                context.requestId())));
    }
}
