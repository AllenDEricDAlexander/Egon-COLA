package top.egon.cola.archetype.source.light.infrastructure.teaching.service.impl;

import top.egon.cola.archetype.source.light.domain.teaching.aggregates.SchoolClassAggregate;
import top.egon.cola.archetype.source.light.domain.teaching.entities.Course;
import top.egon.cola.archetype.source.light.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.light.domain.teaching.service.SchoolClassDomainService;
import top.egon.cola.archetype.source.light.domain.teaching.vos.CourseSchedule;
import top.egon.cola.archetype.source.light.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.light.domain.teaching.vos.Semester;
import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.converter.CoursePOConverter;
import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.converter.SchoolClassPOConverter;
import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.ClassCourseScheduleRepository;
import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.CourseRepository;
import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.SchoolClassRepository;
import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.po.ClassCourseSchedulePO;
import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.po.SchoolClassPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.util.Optional;

/** Business rules and orchestration for the school-class domain service. */
@Slf4j
@Validated
@Service("schoolClassDomainService")
@RequiredArgsConstructor
public class SchoolClassDomainServiceImpl
        implements SchoolClassDomainService {

    @Qualifier("schoolClassRepository")
    private final SchoolClassRepository schoolClassRepository;
    @Qualifier("courseRepository")
    private final CourseRepository courseRepository;
    @Qualifier("classCourseScheduleRepository")
    private final ClassCourseScheduleRepository scheduleRepository;
    @Qualifier("schoolClassPOConverterImpl")
    private final SchoolClassPOConverter schoolClassConverter;
    @Qualifier("coursePOConverterImpl")
    private final CoursePOConverter courseConverter;
    @Qualifier("snowflakeIdGenerator")
    private final LongIdGenerator idGenerator;

    @Override
    public SchoolClass createSchoolClass(String name, Semester semester) {
        return new SchoolClass(new SchoolClassId(idGenerator.nextLongId()), name, semester,
                top.egon.cola.archetype.source.light.domain.teaching.enums.SchoolClassStatus.ACTIVE);
    }

    @Override
    @Transactional
    public SchoolClass save(SchoolClass schoolClass) {
        SchoolClassPO po = schoolClassConverter.toTarget(schoolClass);
        SchoolClassPO current = po.getId() == null ? null : schoolClassRepository.getById(po.getId());
        if (current != null) { schoolClassConverter.updateMetadata(po, current); }
        boolean written = current == null ? schoolClassRepository.save(po) : schoolClassRepository.updateById(po);
        if (!written) { throw new org.springframework.dao.OptimisticLockingFailureException("VERSIONED_WRITE_CONFLICT"); }

        return schoolClassConverter.toSource(po);
    }

    @Override
    public Optional<SchoolClassAggregate> findAggregateById(SchoolClassId schoolClassId) {
        SchoolClassPO po = schoolClassRepository.getById(schoolClassId.value());
        if (po == null) {
            return Optional.empty();
        }
        SchoolClassAggregate aggregate = new SchoolClassAggregate(
                schoolClassConverter.toSource(po));
        scheduleRepository.selectBySchoolClassIdOrderByStartsAt(schoolClassId.value()).forEach(schedule -> {
            Course course = Optional.ofNullable(courseRepository.getById(schedule.getCourseId()))
                    .map(courseConverter::toSource)
                    .orElseThrow(() -> new IllegalStateException("scheduled course not found"));
            aggregate.schedule(course, new CourseSchedule(
                    course.code(), schedule.getStartsAt(), schedule.getEndsAt()));
        });
        return Optional.of(aggregate);
    }

    @Override
    @Transactional
    public void saveAggregate(SchoolClassAggregate aggregate) {
        save(aggregate.schoolClass());
        aggregate.schedules().forEach(schedule -> {
            Course course = courseRepository.selectByCourseCode(schedule.courseCode().value()).stream()
                    .findFirst().map(courseConverter::toSource)
                    .orElseThrow(() -> new IllegalStateException("scheduled course not found"));
            if (!scheduleRepository.save(ClassCourseSchedulePO.builder()
                    .schoolClassId(aggregate.schoolClass().id().value())
                    .courseId(course.id())
                    .startsAt(schedule.startsAt())
                    .endsAt(schedule.endsAt())
                    .build())) { throw new IllegalStateException("INSERT_AFFECTED_ZERO_ROWS"); }
        });
    }

    @Override
    public SchoolClassAggregate schedule(
            SchoolClassAggregate schoolClass, Course course, CourseSchedule schedule) {
        schoolClass.schedule(course, schedule);
        return schoolClass;
    }

}
