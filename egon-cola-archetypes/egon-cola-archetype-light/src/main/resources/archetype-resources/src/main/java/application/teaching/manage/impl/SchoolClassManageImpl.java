package ${package}.application.teaching.manage.impl;

import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.command.ScheduleCourseCommand;
import ${package}.application.teaching.convertor.TeachingApplicationConvertor;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.application.teaching.manage.TeachingUseCaseException;
import ${package}.application.teaching.query.GetSchoolClassQuery;
import ${package}.application.teaching.result.SchoolClassResult;
import ${package}.application.teaching.validators.TeachingApplicationValidator;
import ${package}.domain.teaching.aggregates.SchoolClassAggregate;
import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.exceptions.TeachingDomainException;
import ${package}.domain.teaching.service.CourseDomainService;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.teaching.event.TeachingEventPublisher;
import ${package}.domain.teaching.vos.CourseSchedule;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.teaching.vos.Semester;
import ${package}.domain.teaching.vos.TeachingEvent;
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
