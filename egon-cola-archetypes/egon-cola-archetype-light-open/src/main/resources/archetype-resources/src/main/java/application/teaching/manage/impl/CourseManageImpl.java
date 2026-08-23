package ${package}.application.teaching.manage.impl;

import ${package}.application.teaching.command.CreateCourseCommand;
import ${package}.application.teaching.convertor.TeachingApplicationConvertor;
import ${package}.application.teaching.manage.CourseManage;
import ${package}.application.teaching.manage.TeachingUseCaseException;
import ${package}.application.teaching.query.GetCourseQuery;
import ${package}.application.teaching.result.CourseResult;
import ${package}.application.teaching.validators.TeachingApplicationValidator;
import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.exceptions.TeachingDomainException;
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.domain.teaching.service.CourseCacheService;
import ${package}.domain.teaching.service.CourseDomainService;
import ${package}.domain.teaching.service.TeachingEventPublisher;
import ${package}.domain.teaching.service.TeachingQueryService;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.CourseSnapshot;
import ${package}.domain.teaching.vos.TeachingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Lazy
@RequiredArgsConstructor
public class CourseManageImpl implements CourseManage {
    @Qualifier("courseDomainService")
    private final CourseDomainService courseDomainService;
    @Qualifier("courseRepository")
    private final CourseRepository courseRepository;
    @Qualifier("teachingQueryService")
    private final TeachingQueryService teachingQueryService;
    @Qualifier("courseCacheService")
    private final CourseCacheService courseCacheService;
    @Qualifier("teachingEventPublisher")
    private final TeachingEventPublisher teachingEventPublisher;
    private final TeachingApplicationValidator applicationValidator;
    private final TeachingApplicationConvertor convertor;

    @Override
    @Transactional
    public CourseResult create(CreateCourseCommand command) {
        applicationValidator.validate(command);
        CourseCode code = new CourseCode(command.code());
        teachingQueryService.findExternalCourse(code)
                .orElseThrow(() -> new TeachingUseCaseException(
                        "EXTERNAL_COURSE_NOT_FOUND", "external course not found"));
        try {
            Course saved = courseRepository.save(courseDomainService.createCourse(code, command.name()));
            courseCacheService.evictCourse(saved.id());
            teachingEventPublisher.publish(TeachingEvent.courseCreated(saved.id()));
            return convertor.toResult(saved);
        } catch (TeachingDomainException exception) {
            throw translate(exception);
        }
    }

    @Override
    public CourseResult get(GetCourseQuery query) {
        return courseCacheService.getCourse(query.courseId())
                .map(convertor::toResult)
                .orElseGet(() -> loadAndCache(query.courseId()));
    }

    private CourseResult loadAndCache(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new TeachingUseCaseException("COURSE_NOT_FOUND", "course not found"));
        CourseSnapshot snapshot = convertor.toSnapshot(course);
        courseCacheService.putCourse(snapshot);
        return convertor.toResult(course);
    }

    private TeachingUseCaseException translate(TeachingDomainException exception) {
        return new TeachingUseCaseException(exception.getCode(), exception.getMessage(), exception);
    }
}
