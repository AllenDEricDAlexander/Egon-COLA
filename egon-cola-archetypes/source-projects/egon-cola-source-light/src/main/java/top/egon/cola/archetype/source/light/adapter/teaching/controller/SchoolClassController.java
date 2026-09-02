package top.egon.cola.archetype.source.light.adapter.teaching.controller;

import top.egon.cola.archetype.source.light.adapter.filter.RequestContext;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder;
import top.egon.cola.archetype.source.light.adapter.teaching.convertor.TeachingAdapterConvertor;
import top.egon.cola.archetype.source.light.adapter.teaching.dto.CreateSchoolClassRequest;
import top.egon.cola.archetype.source.light.adapter.teaching.dto.ScheduleCourseRequest;
import top.egon.cola.archetype.source.light.adapter.teaching.validators.TeachingRequestValidator;
import top.egon.cola.archetype.source.light.adapter.teaching.vo.SchoolClassDetailVO;
import top.egon.cola.archetype.source.light.application.teaching.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.light.application.teaching.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.light.application.teaching.query.GetSchoolClassQuery;
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
                schoolClassManage.get(new GetSchoolClassQuery(Long.valueOf(schoolClassId))));
    }

    @PostMapping("/{schoolClassId}/courses/{courseId}/schedule")
    public SchoolClassDetailVO schedule(
            @PathVariable String schoolClassId,
            @PathVariable String courseId,
            @Valid @RequestBody ScheduleCourseRequest request) {
        validator.validateSchedule(request);
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return convertor.toSchoolClass(schoolClassManage.schedule(new ScheduleCourseCommand(
                Long.valueOf(schoolClassId),
                Long.valueOf(courseId),
                request.startsAt(),
                request.endsAt(),
                context.operatorId(),
                context.requestId())));
    }
}
