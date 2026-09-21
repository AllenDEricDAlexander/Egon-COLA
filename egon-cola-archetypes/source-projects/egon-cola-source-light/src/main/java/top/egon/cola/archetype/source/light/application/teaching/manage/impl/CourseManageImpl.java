package top.egon.cola.archetype.source.light.application.teaching.manage.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.archetype.source.light.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.convertor.TeachingApplicationConvertor;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetCourseQuery;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.CourseResult;
import top.egon.cola.archetype.source.light.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.light.common.exception.TeachingDomainException;
import top.egon.cola.archetype.source.light.common.exception.TeachingUseCaseException;
import top.egon.cola.archetype.source.light.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.light.domain.teaching.service.CourseDomainService;
import top.egon.cola.archetype.source.light.domain.teaching.service.CourseIdempotencyService;
import top.egon.cola.archetype.source.light.domain.teaching.service.TeachingEventService;
import top.egon.cola.archetype.source.light.domain.teaching.service.TeachingQueryService;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseCode;
import top.egon.cola.archetype.source.light.domain.teaching.vos.TeachingEvent;

@Service("courseManageImpl")
@Lazy
@RequiredArgsConstructor
@Slf4j
public class CourseManageImpl implements CourseManage {
    @Qualifier("courseDomainService")
    private final CourseDomainService courseDomainService;
    @Qualifier("teachingQueryService")
    private final TeachingQueryService teachingQueryService;
    @Qualifier("teachingEventService")
    private final TeachingEventService teachingEventService;
    @Qualifier("courseIdempotencyService")
    private final CourseIdempotencyService courseIdempotencyService;
    @Qualifier("teachingApplicationValidator")
    private final TeachingApplicationValidator applicationValidator;
    @Qualifier("teachingApplicationConvertorImpl")
    private final TeachingApplicationConvertor convertor;

    @Override
    @Transactional
    public CourseResult create(CreateCourseCommand command) {
        applicationValidator.validate(command);
        claim(command.idempotencyKey());
        CourseCode code = new CourseCode(command.code());
        teachingQueryService.findExternalCourse(code)
                .orElseThrow(() -> new TeachingUseCaseException(
                        "EXTERNAL_COURSE_NOT_FOUND", "external course not found"));
        try {
            Course saved = courseDomainService.save(courseDomainService.createCourse(code, command.name()));
            teachingEventService.publish(TeachingEvent.courseCreated(saved.id()));
            return convertor.toTarget(saved);
        } catch (TeachingDomainException exception) {
            throw translate(exception);
        }
    }

    @Override
    public CourseResult get(GetCourseQuery query) {
        return courseDomainService.findById(query.courseId())
                .map(convertor::toTarget)
                .orElseThrow(() -> new TeachingUseCaseException("COURSE_NOT_FOUND", "course not found"));
    }

    private void claim(String idempotencyKey) {
        if (!courseIdempotencyService.claim(idempotencyKey)) {
            throw new TeachingUseCaseException("DUPLICATE_REQUEST", "request was already processed");
        }
    }

    private TeachingUseCaseException translate(TeachingDomainException exception) {
        return new TeachingUseCaseException(exception.getStatus(), exception.getMessage(), exception);
    }
}
