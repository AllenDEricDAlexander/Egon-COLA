package top.egon.cola.archetype.source.lightopen.adapter.teaching.controller;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.archetype.source.lightopen.adapter.filter.RequestContext;
import top.egon.cola.archetype.source.lightopen.adapter.filter.RequestContextHolder;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.convertor.TeachingAdapterConvertor;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.dto.CreateSchoolClassRequest;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.dto.ScheduleCourseRequest;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.validators.TeachingRequestValidator;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.vo.SchoolClassDetailVO;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.query.GetSchoolClassQuery;
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
@Slf4j
public class SchoolClassController {
    private final SchoolClassManage schoolClassManage;
    private final TeachingRequestValidator validator;
    private final TeachingAdapterConvertor convertor;

    @PostMapping
    public SchoolClassDetailVO create(@Valid @RequestBody CreateSchoolClassRequest request) {
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
    public SchoolClassDetailVO schedule(
            @PathVariable String schoolClassId,
            @PathVariable String courseId,
            @Valid @RequestBody ScheduleCourseRequest request) {
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
