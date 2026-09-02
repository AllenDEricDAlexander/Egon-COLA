package top.egon.cola.archetype.source.light.adapter.teaching.controller;

import top.egon.cola.archetype.source.light.adapter.filter.RequestContext;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder;
import top.egon.cola.archetype.source.light.adapter.teaching.convertor.TeachingAdapterConvertor;
import top.egon.cola.archetype.source.light.adapter.teaching.dto.CreateCourseRequest;
import top.egon.cola.archetype.source.light.adapter.teaching.vo.CourseDetailVO;
import top.egon.cola.archetype.source.light.application.teaching.command.CreateCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.light.application.teaching.query.GetCourseQuery;
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
public class CourseController {
    private final CourseManage courseManage;
    private final TeachingAdapterConvertor convertor;

    @PostMapping
    public CourseDetailVO create(@Valid @RequestBody CreateCourseRequest request) {
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return convertor.toCourse(courseManage.create(new CreateCourseCommand(
                request.code(), request.name(), context.operatorId(), context.requestId())));
    }

    @GetMapping("/{courseId}")
    public CourseDetailVO get(@PathVariable String courseId) {
        return convertor.toCourse(courseManage.get(new GetCourseQuery(Long.valueOf(courseId))));
    }
}
