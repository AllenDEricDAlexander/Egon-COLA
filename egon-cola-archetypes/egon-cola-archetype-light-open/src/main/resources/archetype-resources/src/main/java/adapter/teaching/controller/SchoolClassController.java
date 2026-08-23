package ${package}.adapter.teaching.controller;

import ${package}.adapter.filter.RequestContext;
import ${package}.adapter.filter.RequestContextHolder;
import ${package}.adapter.teaching.convertor.TeachingAdapterConvertor;
import ${package}.adapter.teaching.dto.CreateSchoolClassRequest;
import ${package}.adapter.teaching.dto.ScheduleCourseRequest;
import ${package}.adapter.teaching.validators.TeachingRequestValidator;
import ${package}.adapter.teaching.vo.SchoolClassDetailVO;
import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.command.ScheduleCourseCommand;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.application.teaching.query.GetSchoolClassQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/school-classes")
@RequiredArgsConstructor
public class SchoolClassController {
    private final SchoolClassManage schoolClassManage;
    private final TeachingRequestValidator validator;
    private final TeachingAdapterConvertor convertor;

    @PostMapping
    public SchoolClassDetailVO create(@Valid @RequestBody CreateSchoolClassRequest request) {
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return convertor.toSchoolClass(schoolClassManage.create(new CreateSchoolClassCommand(
                request.name(), request.semester(), context.operatorId(), context.requestId())));
    }

    @GetMapping("/{schoolClassId}")
    public SchoolClassDetailVO get(@PathVariable String schoolClassId) {
        return convertor.toSchoolClass(
                schoolClassManage.get(new GetSchoolClassQuery(schoolClassId)));
    }

    @PostMapping("/{schoolClassId}/courses/{courseId}/schedule")
    public SchoolClassDetailVO schedule(
            @PathVariable String schoolClassId,
            @PathVariable String courseId,
            @Valid @RequestBody ScheduleCourseRequest request) {
        validator.validateSchedule(request);
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return convertor.toSchoolClass(schoolClassManage.schedule(new ScheduleCourseCommand(
                schoolClassId,
                courseId,
                request.startsAt(),
                request.endsAt(),
                context.operatorId(),
                context.requestId())));
    }
}
