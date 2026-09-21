package top.egon.cola.archetype.source.lightopen.adapter.teaching.controller;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.archetype.source.lightopen.adapter.filter.RequestContext;
import top.egon.cola.archetype.source.lightopen.adapter.filter.RequestContextHolder;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.convertor.TeachingAdapterConvertor;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.dto.CreateCourseRequest;
import top.egon.cola.archetype.source.lightopen.adapter.teaching.pojo.vo.CourseDetailVO;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.query.GetCourseQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("teachingDomainCourseController")
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseController {
    private final CourseManage courseManage;
    private final TeachingAdapterConvertor convertor;

    @PostMapping
    public CourseDetailVO create(@Valid @RequestBody CreateCourseRequest request) {
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return convertor.toTarget(courseManage.create(new CreateCourseCommand(
                request.code(), request.name(), context.operatorId(), context.requestId())));
    }

    @GetMapping("/{courseId}")
    public CourseDetailVO get(@PathVariable String courseId) {
        return convertor.toTarget(courseManage.get(new GetCourseQuery(Long.valueOf(courseId))));
    }
}
