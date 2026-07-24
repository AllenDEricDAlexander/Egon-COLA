package ${package}.infrastructure.teaching.repo.impl;

import ${package}.domain.teaching.aggregates.SchoolClassAggregate;
import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.repos.SchoolClassRepository;
import ${package}.domain.teaching.vos.CourseSchedule;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.infrastructure.teaching.repo.converter.CoursePOConverter;
import ${package}.infrastructure.teaching.repo.converter.SchoolClassPOConverter;
import ${package}.infrastructure.teaching.repo.jpa.ClassCourseScheduleJpaRepository;
import ${package}.infrastructure.teaching.repo.jpa.CourseJpaRepository;
import ${package}.infrastructure.teaching.repo.jpa.SchoolClassJpaRepository;
import ${package}.infrastructure.teaching.repo.po.ClassCourseSchedulePO;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import top.egon.cola.component.common.id.generator.IdGenerator;

@Repository("schoolClassRepository")
@RequiredArgsConstructor
public class SchoolClassRepositoryImpl implements SchoolClassRepository {
    private final SchoolClassJpaRepository schoolClassJpaRepository;
    private final CourseJpaRepository courseJpaRepository;
    private final ClassCourseScheduleJpaRepository scheduleJpaRepository;
    private final SchoolClassPOConverter schoolClassConverter;
    private final CoursePOConverter courseConverter;
    private final IdGenerator idGenerator;
    private final EntityManager entityManager;

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        return schoolClassConverter.toDomain(
                schoolClassJpaRepository.save(schoolClassConverter.toPO(schoolClass)));
    }

    @Override
    public Optional<SchoolClassAggregate> findAggregateById(SchoolClassId schoolClassId) {
        return schoolClassJpaRepository.findById(schoolClassId.value()).map(schoolClassPO -> {
            SchoolClassAggregate aggregate = new SchoolClassAggregate(
                    schoolClassConverter.toDomain(schoolClassPO));
            scheduleJpaRepository.findBySchoolClassIdOrderByStartsAt(schoolClassId.value())
                    .forEach(schedulePO -> restoreSchedule(aggregate, schedulePO));
            return aggregate;
        });
    }

    @Override
    @Transactional
    public void saveAggregate(SchoolClassAggregate aggregate) {
        save(aggregate.schoolClass());
        aggregate.schedules().forEach(schedule -> {
            Course course = courseJpaRepository.findByCourseCode(schedule.courseCode().value())
                    .map(courseConverter::toDomain)
                    .orElseThrow(() -> new IllegalStateException("scheduled course not found"));
            entityManager.persist(new ClassCourseSchedulePO(
                    idGenerator.nextId(),
                    aggregate.schoolClass().id().value(),
                    course.id(),
                    schedule.startsAt(),
                    schedule.endsAt(),
                    Instant.now()));
        });
        entityManager.flush();
    }

    private void restoreSchedule(
            SchoolClassAggregate aggregate, ClassCourseSchedulePO schedulePO) {
        Course course = courseJpaRepository.findById(schedulePO.getCourseId())
                .map(courseConverter::toDomain)
                .orElseThrow(() -> new IllegalStateException("scheduled course not found"));
        aggregate.schedule(course, new CourseSchedule(
                course.code(), schedulePO.getStartsAt(), schedulePO.getEndsAt()));
    }
}
