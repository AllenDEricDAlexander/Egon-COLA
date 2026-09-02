package top.egon.cola.archetype.source.lightopen.application.teaching.manage.impl;

import top.egon.cola.archetype.source.lightopen.application.teaching.command.CreateCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.convertor.TeachingApplicationConvertor;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.TeachingUseCaseException;
import top.egon.cola.archetype.source.lightopen.application.teaching.query.GetCourseQuery;
import top.egon.cola.archetype.source.lightopen.application.teaching.result.CourseResult;
import top.egon.cola.archetype.source.lightopen.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.lightopen.domain.teaching.exceptions.TeachingDomainException;
import top.egon.cola.archetype.source.lightopen.domain.teaching.client.CourseCachePort;
import top.egon.cola.archetype.source.lightopen.domain.teaching.service.CourseDomainService;
import top.egon.cola.archetype.source.lightopen.domain.teaching.event.TeachingEventPublisher;
import top.egon.cola.archetype.source.lightopen.domain.teaching.gateway.TeachingQueryGateway;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseSnapshot;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.TeachingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("courseManageImpl")
@Lazy
@RequiredArgsConstructor
@Slf4j
public class CourseManageImpl implements CourseManage {
    @Qualifier("courseDomainService")
    private final CourseDomainService<?> courseDomainService;
    @Qualifier("teachingQueryGateway")
    private final TeachingQueryGateway teachingQueryGateway;
    @Qualifier("courseCachePort")
    private final CourseCachePort courseCachePort;
    @Qualifier("teachingEventPublisher")
    private final TeachingEventPublisher teachingEventPublisher;
    @Qualifier("teachingApplicationValidator")
    private final TeachingApplicationValidator applicationValidator;
    @Qualifier("teachingApplicationConvertor")
    private final TeachingApplicationConvertor convertor;

    @Override
    @Transactional
    public CourseResult create(CreateCourseCommand command) {
        applicationValidator.validate(command);
        CourseCode code = new CourseCode(command.code());
        teachingQueryGateway.findExternalCourse(code)
                .orElseThrow(() -> new TeachingUseCaseException(
                        "EXTERNAL_COURSE_NOT_FOUND", "external course not found"));
        try {
            Course saved = courseDomainService.save(courseDomainService.createCourse(code, command.name()));
            courseCachePort.evictCourse(saved.id());
            teachingEventPublisher.publish(TeachingEvent.courseCreated(saved.id()));
            return convertor.toResult(saved);
        } catch (TeachingDomainException exception) {
            throw translate(exception);
        }
    }

    @Override
    public CourseResult get(GetCourseQuery query) {
        return courseCachePort.getCourse(query.courseId())
                .map(convertor::toResult)
                .orElseGet(() -> loadAndCache(query.courseId()));
    }

    private CourseResult loadAndCache(Long courseId) {
        Course course = courseDomainService.findById(courseId)
                .orElseThrow(() -> new TeachingUseCaseException("COURSE_NOT_FOUND", "course not found"));
        CourseSnapshot snapshot = convertor.toSnapshot(course);
        courseCachePort.putCourse(snapshot);
        return convertor.toResult(course);
    }

    private TeachingUseCaseException translate(TeachingDomainException exception) {
        return new TeachingUseCaseException(exception.getCode(), exception.getMessage(), exception);
    }
}
