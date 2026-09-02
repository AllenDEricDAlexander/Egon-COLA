package top.egon.cola.archetype.source.lightopen.application.teaching.manage.impl;

import top.egon.cola.archetype.source.lightopen.application.teaching.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.command.ScheduleCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.convertor.TeachingApplicationConvertor;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.TeachingUseCaseException;
import top.egon.cola.archetype.source.lightopen.application.teaching.query.GetSchoolClassQuery;
import top.egon.cola.archetype.source.lightopen.application.teaching.result.SchoolClassResult;
import top.egon.cola.archetype.source.lightopen.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.lightopen.domain.teaching.aggregates.SchoolClassAggregate;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.lightopen.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.lightopen.domain.teaching.exceptions.TeachingDomainException;
import top.egon.cola.archetype.source.lightopen.domain.teaching.service.CourseDomainService;
import top.egon.cola.archetype.source.lightopen.domain.teaching.service.SchoolClassDomainService;
import top.egon.cola.archetype.source.lightopen.domain.teaching.event.TeachingEventPublisher;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.CourseSchedule;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.Semester;
import top.egon.cola.archetype.source.lightopen.domain.teaching.vos.TeachingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("schoolClassManageImpl")
@Lazy
@RequiredArgsConstructor
@Slf4j
public class SchoolClassManageImpl implements SchoolClassManage {
    @Qualifier("schoolClassDomainService")
    private final SchoolClassDomainService<?> schoolClassDomainService;
    @Qualifier("courseDomainService")
    private final CourseDomainService<?> courseDomainService;
    @Qualifier("teachingEventPublisher")
    private final TeachingEventPublisher teachingEventPublisher;
    @Qualifier("teachingApplicationValidator")
    private final TeachingApplicationValidator applicationValidator;
    @Qualifier("teachingApplicationConvertor")
    private final TeachingApplicationConvertor convertor;

    @Override
    @Transactional
    public SchoolClassResult create(CreateSchoolClassCommand command) {
        applicationValidator.validate(command);
        try {
            SchoolClass saved = schoolClassDomainService.save(
                    schoolClassDomainService.createSchoolClass(command.name(), new Semester(command.semester())));
            teachingEventPublisher.publish(TeachingEvent.classCreated(saved.id().value()));
            return convertor.toResult(saved);
        } catch (TeachingDomainException exception) {
            throw translate(exception);
        }
    }

    @Override
    @Transactional
    public SchoolClassResult schedule(ScheduleCourseCommand command) {
        applicationValidator.validate(command);
        SchoolClassAggregate aggregate = schoolClassDomainService
                .findAggregateById(new SchoolClassId(command.schoolClassId()))
                .orElseThrow(() -> new TeachingUseCaseException("CLASS_NOT_FOUND", "class not found"));
        Course course = courseDomainService.findById(command.courseId())
                .orElseThrow(() -> new TeachingUseCaseException("COURSE_NOT_FOUND", "course not found"));
        CourseSchedule schedule = convertor.toSchedule(command, course);
        try {
            SchoolClassAggregate scheduled = schoolClassDomainService.schedule(aggregate, course, schedule);
            schoolClassDomainService.saveAggregate(scheduled);
            teachingEventPublisher.publish(TeachingEvent.courseScheduled(command.schoolClassId()));
            return convertor.toResult(scheduled);
        } catch (TeachingDomainException exception) {
            throw translate(exception);
        }
    }

    @Override
    public SchoolClassResult get(GetSchoolClassQuery query) {
        SchoolClassAggregate aggregate = schoolClassDomainService
                .findAggregateById(new SchoolClassId(query.schoolClassId()))
                .orElseThrow(() -> new TeachingUseCaseException("CLASS_NOT_FOUND", "class not found"));
        return convertor.toResult(aggregate);
    }

    private TeachingUseCaseException translate(TeachingDomainException exception) {
        return new TeachingUseCaseException(exception.getCode(), exception.getMessage(), exception);
    }
}
