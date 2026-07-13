#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.course.manage.impl;

import ${package}.application.course.manage.CourseManage;
import ${package}.application.course.command.CreateCourseCommand;
import ${package}.application.course.command.ScheduleCourseCommand;
import ${package}.application.course.converter.CourseApplicationConverter;
import ${package}.application.exceptions.ApplicationErrorCode;
import ${package}.application.exceptions.ApplicationException;
import ${package}.application.course.query.GetCourseQuery;
import ${package}.application.course.query.PageCourseQuery;
import ${package}.application.course.result.CourseResult;
import ${package}.application.course.result.CourseScheduleResult;
import ${package}.application.result.PageResult;
import ${package}.application.course.validators.CourseApplicationValidator;
import ${package}.domain.common.Page;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.event.CourseEventPublisher;
import ${package}.domain.course.repos.CourseRepository;
import ${package}.domain.course.repos.CourseScheduleRepository;
import ${package}.domain.course.service.CourseDomainService;
import ${package}.domain.course.vos.CourseCode;
import ${package}.domain.course.vos.CourseId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service("courseManage")
@Validated
@RequiredArgsConstructor
public class CourseManageImpl implements CourseManage {

    private final CourseRepository courseRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final CourseEventPublisher courseEventPublisher;

    private final CourseDomainService courseDomainService;

    private final CourseApplicationConverter converter;

    private final CourseApplicationValidator validator;

    @Override
    @Transactional
    public CourseResult create(CreateCourseCommand command) {
        validator.require(command != null, "create course command is required");
        CourseCode code = new CourseCode(command.code());
        if (courseRepository.existsByCode(code)) {
            throw new ApplicationException(
                    ApplicationErrorCode.COURSE_CODE_DUPLICATED, "course code already exists");
        }
        Course course = courseDomainService.createCourse(
                CourseId.newId().value(), code, command.name(), command.credit());
        return converter.toResult(courseRepository.save(course));
    }

    @Override
    @Transactional
    public CourseScheduleResult schedule(ScheduleCourseCommand command) {
        validator.require(command != null, "schedule course command is required");
        CourseId courseId = new CourseId(command.courseId());
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.COURSE_NOT_FOUND, "course not found"));
        CourseSchedule schedule = courseDomainService.scheduleCourse(
                java.util.UUID.randomUUID().toString(), course, command.classId(),
                command.startsAt(), command.endsAt(), courseScheduleRepository.findOverlapping(
                        courseId, command.classId(), command.startsAt(), command.endsAt()));
        CourseSchedule saved = courseScheduleRepository.save(schedule);
        courseEventPublisher.courseScheduled(saved);
        return converter.toResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResult get(GetCourseQuery query) {
        validator.require(query != null, "get course query is required");
        return courseRepository.findById(new CourseId(query.courseId()))
                .map(converter::toResult)
                .orElseThrow(() -> new ApplicationException(
                        ApplicationErrorCode.COURSE_NOT_FOUND, "course not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CourseResult> page(PageCourseQuery query) {
        validator.require(
                query != null && query.currentPage() > 0 && query.pageSize() > 0,
                "positive page parameters are required");
        Page<Course> page = courseRepository.findPage(query.currentPage(), query.pageSize());
        return PageResult.of(
                page.records().stream().map(converter::toResult).toList(),
                page.currentPage(), page.totalPages(), page.pageSize(), page.totalCount());
    }

}
