package top.egon.cola.archetype.source.lightopen.adapter.teaching.controller;

import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.archetype.source.lightopen.adapter.filter.RequestContext;
import top.egon.cola.archetype.source.lightopen.adapter.filter.RequestContextHolder;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.convertor.TeachingAdapterConvertor;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.dto.CreateSchoolClassDTO;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.dto.ScheduleCourseDTO;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.validators.TeachingRequestValidator;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.vo.SchoolClassDetailVO;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.SchoolClassManage;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.query.GetSchoolClassQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("schoolClassController")
@RequestMapping("/api/school-classes")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class SchoolClassController {
    @Qualifier("schoolClassManageImpl")
    private final SchoolClassManage schoolClassManage;
    @Qualifier("teachingRequestValidator")
    private final TeachingRequestValidator validator;
    @Qualifier("teachingAdapterConvertorImpl")
    private final TeachingAdapterConvertor convertor;

    @PostMapping
    public SchoolClassDetailVO create(@Valid @RequestBody CreateSchoolClassDTO request) {
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return convertor.toSchoolClassDetail(schoolClassManage.create(new CreateSchoolClassCommand(
                request.name(), request.semester(), context.operatorId(), context.requestId())));
    }

    @GetMapping("/{schoolClassId}")
    public SchoolClassDetailVO get(@PathVariable String schoolClassId) {
        return convertor.toSchoolClassDetail(
                schoolClassManage.get(new GetSchoolClassQuery(Long.valueOf(schoolClassId))));
    }

    @PostMapping("/{schoolClassId}/courses/{courseId}/schedule")
    @Operation(operationId = "schedule", description = "Offset or Z datetime input; windows are truncated to microseconds before validation and persistence. Returns the existing ResultRecord envelope with SchoolClassDetailVO in data.")
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
    public SchoolClassDetailVO schedule(
            @PathVariable String schoolClassId,
            @PathVariable String courseId,
            @Valid @RequestBody ScheduleCourseDTO request) {
        validator.validateSchedule(request);
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return convertor.toSchoolClassDetail(schoolClassManage.schedule(new ScheduleCourseCommand(
                Long.valueOf(schoolClassId),
                Long.valueOf(courseId),
                request.startsAt(),
                request.endsAt(),
                context.operatorId(),
                context.requestId())));
    }
}
