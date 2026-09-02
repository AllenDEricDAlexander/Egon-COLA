package top.egon.cola.archetype.source.service.application.course.manage.impl;

import top.egon.cola.archetype.source.service.application.course.command.CreateCourseCommand;
import top.egon.cola.archetype.source.service.application.course.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.service.application.course.converter.CourseApplicationConverter;
import top.egon.cola.archetype.source.service.application.course.manage.CourseManage;
import top.egon.cola.archetype.source.service.application.course.query.GetCourseQuery;
import top.egon.cola.archetype.source.service.application.course.query.PageCourseQuery;
import top.egon.cola.archetype.source.service.application.course.result.CourseResult;
import top.egon.cola.archetype.source.service.application.course.result.CourseScheduleResult;
import top.egon.cola.archetype.source.service.application.course.validators.CourseApplicationValidator;
import top.egon.cola.archetype.source.service.application.exceptions.ApplicationErrorCode;
import top.egon.cola.archetype.source.service.application.exceptions.ApplicationException;
import top.egon.cola.archetype.source.service.application.result.PageResult;
import top.egon.cola.archetype.source.service.domain.common.Page;
import top.egon.cola.archetype.source.service.domain.course.entities.Course;
import top.egon.cola.archetype.source.service.domain.course.entities.CourseSchedule;
import top.egon.cola.archetype.source.service.domain.course.event.CourseEventPublisher;
import top.egon.cola.archetype.source.service.domain.course.service.CourseDomainService;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseCode;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service("courseManage")
@Validated
@RequiredArgsConstructor
public class CourseManageImpl implements CourseManage {

    private final CourseEventPublisher courseEventPublisher;
    private final CourseDomainService<?> courseDomainService;
    private final CourseApplicationConverter converter;
    private final CourseApplicationValidator validator;

    @Override
    @Transactional
    public CourseResult create(CreateCourseCommand command) {
        validator.require(command != null, "create course command is required");
        CourseCode code = new CourseCode(command.code());
        if (courseDomainService.existsByCode(code)) {
            throw new ApplicationException(
                    ApplicationErrorCode.COURSE_CODE_DUPLICATED, "course code already exists");
        }
        Course course = courseDomainService.createCourse(code, command.name(), command.credit());
        return converter.toResult(courseDomainService.save(course));
    }

    @Override
    @Transactional
    public CourseScheduleResult schedule(ScheduleCourseCommand command) {
        validator.require(command != null, "schedule course command is required");
        CourseId courseId = new CourseId(command.courseId());
        Course course = courseDomainService.findById(courseId)
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.COURSE_NOT_FOUND, "course not found"));
        CourseSchedule schedule = courseDomainService.scheduleCourse(
                course, command.classId(), command.startsAt(), command.endsAt(),
                courseDomainService.findOverlapping(
                        courseId, command.classId(), command.startsAt(), command.endsAt()));
        CourseSchedule saved = courseDomainService.saveSchedule(schedule);
        courseEventPublisher.courseScheduled(saved);
        return converter.toResult(saved);
    }

    @Override
    public CourseResult get(GetCourseQuery query) {
        validator.require(query != null, "get course query is required");
        return courseDomainService.findById(new CourseId(query.courseId()))
                .map(converter::toResult)
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.COURSE_NOT_FOUND, "course not found"));
    }

    @Override
    public PageResult<CourseResult> page(PageCourseQuery query) {
        validator.require(
                query != null && query.currentPage() > 0 && query.pageSize() > 0,
                "positive page parameters are required");
        Page<Course> page = courseDomainService.findPage(query.currentPage(), query.pageSize());
        return PageResult.of(
                page.records().stream().map(converter::toResult).toList(),
                page.currentPage(), page.totalPages(), page.pageSize(), page.totalCount());
    }
}
