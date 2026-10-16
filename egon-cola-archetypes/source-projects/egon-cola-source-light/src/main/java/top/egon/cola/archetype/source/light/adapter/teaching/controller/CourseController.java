package top.egon.cola.archetype.source.light.adapter.teaching.controller;

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
import top.egon.cola.archetype.source.light.adapter.teaching.pojo.dto.CreateCourseDTO;
import top.egon.cola.archetype.source.light.adapter.teaching.pojo.vo.CourseDetailVO;
import top.egon.cola.archetype.source.light.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetCourseQuery;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties;
import top.egon.cola.platform.tianquan.jianshen.starter.field.FieldWriteAuthorizationCommand;
import top.egon.cola.platform.tianquan.jianshen.starter.field.Rbac3FieldWriteAuthorizationComponent;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RBACAPIResource;

@RestController("teachingDomainCourseController")
@RequestMapping("/api/courses")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
@SecurityRequirement(name = "nativeBearer")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Authentication required", headers = {@Header(name = "WWW-Authenticate", required = true, schema = @Schema(type = "string", allowableValues = {"Bearer"})), @Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))}, content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError"))),
        @ApiResponse(responseCode = "403", description = "Authorization denied", headers = {@Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))}, content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError"))),
        @ApiResponse(responseCode = "503", description = "Authorization unavailable", headers = {@Header(name = "Cache-Control", required = true, schema = @Schema(type = "string", allowableValues = {"no-store"}))}, content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/NativeSecurityError")))
})
public class CourseController {
    @Qualifier("courseManageImpl")
    private final CourseManage courseManage;
    @Qualifier("teachingAdapterConvertorImpl")
    private final TeachingAdapterConvertor convertor;
    @Qualifier("rbac3FieldWriteAuthorizationComponent")
    private final Rbac3FieldWriteAuthorizationComponent fieldWrite;
    @Qualifier("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties")
    private final Rbac3StarterProperties authorizationProperties;

    @PostMapping
    @Operation(operationId = "nativeLightCourseCreateV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-light:course:create')")
    @RBACAPIResource(code = "egon-cola-source-light.api.course.create", permission = "egon-cola-source-light:course:create", name = "Create course")
    public CourseDetailVO create(@Valid @RequestBody CreateCourseDTO request) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-light:course:create", authorizationProperties.getSystemCode(),
                "egon-cola-source-light.api.course.create", Set.of("code", "name")));
        RequestContext context = RequestContextHolder.getRequired();
        return convertor.toTarget(courseManage.create(new CreateCourseCommand(
                request.code(), request.name(), context.operatorId(), context.requestId())));
    }

    @GetMapping("/{courseId}")
    @Operation(operationId = "nativeLightCourseGetV1")
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-light:course:read')")
    @RBACAPIResource(code = "egon-cola-source-light.api.course.read", permission = "egon-cola-source-light:course:read", name = "Read course")
    public CourseDetailVO get(@PathVariable String courseId) {
        return convertor.toTarget(courseManage.get(new GetCourseQuery(Long.valueOf(courseId))));
    }
}
