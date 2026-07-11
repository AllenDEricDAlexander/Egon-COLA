#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.course.impl;

import ${package}.application.manage.course.CourseManage;
import ${package}.application.command.course.CreateCourseCommand;
import ${package}.application.command.course.ScheduleCourseCommand;
import ${package}.application.converter.course.CourseApplicationConverter;
import ${package}.application.exceptions.ApplicationErrorCode;
import ${package}.application.exceptions.ApplicationException;
import ${package}.application.query.course.GetCourseQuery;
import ${package}.application.query.course.PageCourseQuery;
import ${package}.application.result.course.CourseResult;
import ${package}.application.result.course.CourseScheduleResult;
import ${package}.application.validators.course.CourseApplicationValidator;
import ${package}.common.constants.ErrorCodes;
import ${package}.common.exception.BizException;
import ${package}.common.exception.NotFoundException;
import ${package}.common.util.IdGenerator;
import ${package}.domain.client.course.CourseClient;
import ${package}.domain.common.Page;
import ${package}.domain.entities.course.Course;
import ${package}.domain.entities.course.CourseSchedule;
import ${package}.domain.event.course.CourseEventPublisher;
import ${package}.domain.repos.course.CourseRepository;
import ${package}.domain.repos.course.CourseScheduleRepository;
import ${package}.domain.service.course.CourseDomainService;
import ${package}.domain.vos.course.CourseCode;
import ${package}.domain.vos.course.CourseId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service("courseManage")
@Validated
@RequiredArgsConstructor
public class CourseManageImpl implements CourseManage {

    @Qualifier("courseClientImpl")
    private final CourseClient courseClient;

    @Qualifier("courseRepositoryImpl")
    private final CourseRepository courseRepository;

    private final ObjectProvider<CourseScheduleRepository> scheduleRepositories;

    private final ObjectProvider<CourseEventPublisher> eventPublishers;

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
        CourseScheduleRepository schedules = scheduleRepositories.getObject();
        CourseSchedule schedule = courseDomainService.scheduleCourse(
                java.util.UUID.randomUUID().toString(), course, command.classId(),
                command.startsAt(), command.endsAt(), schedules.findOverlapping(
                        courseId, command.classId(), command.startsAt(), command.endsAt()));
        CourseSchedule saved = schedules.save(schedule);
        eventPublishers.getObject().courseScheduled(saved);
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
    public Page<CourseResult> page(PageCourseQuery query) {
        validator.require(
                query != null && query.currentPage() > 0 && query.pageSize() > 0,
                "positive page parameters are required");
        Page<Course> page = courseRepository.findPage(query.currentPage(), query.pageSize());
        return Page.of(
                page.records().stream().map(converter::toResult).toList(),
                page.currentPage(), page.totalPages(), page.pageSize(), page.totalCount());
    }

    @Override
    @Transactional
    public Course create(String name, int credit) {
        Course course = Course.create(IdGenerator.nextId(), name, credit);
        if (courseClient.existsByName(name)) {
            throw new BizException(ErrorCodes.COURSE_NAME_DUPLICATED, "course name already exists");
        }
        return courseClient.save(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Course getById(String courseId) {
        return courseClient.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.COURSE_NOT_FOUND, "course not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Course> getPage(int currentPage, int pageSize) {
        return courseClient.findPage(currentPage, pageSize);
    }
}
