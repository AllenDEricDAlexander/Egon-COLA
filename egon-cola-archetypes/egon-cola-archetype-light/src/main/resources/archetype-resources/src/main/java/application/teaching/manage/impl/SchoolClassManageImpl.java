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
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.domain.teaching.repos.SchoolClassRepository;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.teaching.service.TeachingEventPublisher;
import ${package}.domain.teaching.vos.CourseSchedule;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.teaching.vos.Semester;
import ${package}.domain.teaching.vos.TeachingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Lazy
@RequiredArgsConstructor
public class SchoolClassManageImpl implements SchoolClassManage {
    @Qualifier("schoolClassDomainService")
    private final SchoolClassDomainService schoolClassDomainService;
    @Qualifier("schoolClassRepository")
    private final SchoolClassRepository schoolClassRepository;
    @Qualifier("courseRepository")
    private final CourseRepository courseRepository;
    @Qualifier("teachingEventPublisher")
    private final TeachingEventPublisher teachingEventPublisher;
    private final TeachingApplicationValidator applicationValidator;
    private final TeachingApplicationConvertor convertor;

    @Override
    @Transactional
    public SchoolClassResult create(CreateSchoolClassCommand command) {
        applicationValidator.validate(command);
        try {
            SchoolClass saved = schoolClassRepository.save(
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
        SchoolClassAggregate aggregate = schoolClassRepository
                .findAggregateById(new SchoolClassId(command.schoolClassId()))
                .orElseThrow(() -> new TeachingUseCaseException("CLASS_NOT_FOUND", "class not found"));
        Course course = courseRepository.findById(command.courseId())
                .orElseThrow(() -> new TeachingUseCaseException("COURSE_NOT_FOUND", "course not found"));
        CourseSchedule schedule = convertor.toSchedule(command, course);
        try {
            SchoolClassAggregate scheduled = schoolClassDomainService.schedule(aggregate, course, schedule);
            schoolClassRepository.saveAggregate(scheduled);
            teachingEventPublisher.publish(TeachingEvent.courseScheduled(command.schoolClassId()));
            return convertor.toResult(scheduled);
        } catch (TeachingDomainException exception) {
            throw translate(exception);
        }
    }

    @Override
    public SchoolClassResult get(GetSchoolClassQuery query) {
        SchoolClassAggregate aggregate = schoolClassRepository
                .findAggregateById(new SchoolClassId(query.schoolClassId()))
                .orElseThrow(() -> new TeachingUseCaseException("CLASS_NOT_FOUND", "class not found"));
        return convertor.toResult(aggregate);
    }

    private TeachingUseCaseException translate(TeachingDomainException exception) {
        return new TeachingUseCaseException(exception.getCode(), exception.getMessage(), exception);
    }
}
