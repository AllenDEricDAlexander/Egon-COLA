package top.egon.cola.archetype.source.light.application.teaching.manage.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.archetype.source.light.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.pojo.convertor.TeachingApplicationConvertor;
import top.egon.cola.archetype.source.light.application.teaching.pojo.query.GetSchoolClassQuery;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.SchoolClassResult;
import top.egon.cola.archetype.source.light.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.light.common.exception.TeachingDomainException;
import top.egon.cola.archetype.source.light.common.exception.TeachingUseCaseException;
import top.egon.cola.archetype.source.light.domain.teaching.aggregates.SchoolClassAggregate;
import top.egon.cola.archetype.source.light.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.light.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.light.domain.teaching.service.CourseDomainService;
import top.egon.cola.archetype.source.light.domain.teaching.service.CourseIdempotencyService;
import top.egon.cola.archetype.source.light.domain.teaching.service.SchoolClassDomainService;
import top.egon.cola.archetype.source.light.domain.teaching.service.TeachingEventService;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseSchedule;
import top.egon.cola.archetype.source.light.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.light.domain.teaching.vos.Semester;
import top.egon.cola.archetype.source.light.domain.teaching.vos.TeachingEvent;

@Service("schoolClassManageImpl")
@Lazy
@RequiredArgsConstructor
@Slf4j
public class SchoolClassManageImpl implements SchoolClassManage {
    @Qualifier("schoolClassDomainService")
    private final SchoolClassDomainService schoolClassDomainService;
    @Qualifier("courseDomainService")
    private final CourseDomainService courseDomainService;
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
    public SchoolClassResult create(CreateSchoolClassCommand command) {
        applicationValidator.validate(command);
        claim(command.idempotencyKey());
        try {
            SchoolClass saved = schoolClassDomainService.save(
                    schoolClassDomainService.createSchoolClass(command.name(), new Semester(command.semester())));
            teachingEventService.publish(TeachingEvent.classCreated(saved.id().value()));
            return convertor.toSchoolClassResult(saved);
        } catch (TeachingDomainException exception) {
            throw translate(exception);
        }
    }

    @Override
    @Transactional
    public SchoolClassResult schedule(ScheduleCourseCommand command) {
        applicationValidator.validate(command);
        claim(command.idempotencyKey());
        SchoolClassAggregate aggregate = schoolClassDomainService
                .findAggregateById(new SchoolClassId(command.schoolClassId()))
                .orElseThrow(() -> new TeachingUseCaseException("CLASS_NOT_FOUND", "class not found"));
        Course course = courseDomainService.findById(command.courseId())
                .orElseThrow(() -> new TeachingUseCaseException("COURSE_NOT_FOUND", "course not found"));
        CourseSchedule schedule = convertor.toSchedule(command, course);
        try {
            SchoolClassAggregate scheduled = schoolClassDomainService.schedule(aggregate, course, schedule);
            schoolClassDomainService.saveAggregate(scheduled);
            teachingEventService.publish(TeachingEvent.courseScheduled(command.schoolClassId()));
            return convertor.toSchoolClassResult(scheduled);
        } catch (TeachingDomainException exception) {
            throw translate(exception);
        }
    }

    @Override
    public SchoolClassResult get(GetSchoolClassQuery query) {
        SchoolClassAggregate aggregate = schoolClassDomainService
                .findAggregateById(new SchoolClassId(query.schoolClassId()))
                .orElseThrow(() -> new TeachingUseCaseException("CLASS_NOT_FOUND", "class not found"));
        return convertor.toSchoolClassResult(aggregate);
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
