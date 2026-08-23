package ${package}.infrastructure.teaching.repo.impl;

import ${package}.domain.teaching.aggregates.SchoolClassAggregate;
import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.repos.SchoolClassRepository;
import ${package}.domain.teaching.vos.CourseSchedule;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.infrastructure.teaching.repo.converter.CoursePOConverter;
import ${package}.infrastructure.teaching.repo.converter.SchoolClassPOConverter;
import ${package}.infrastructure.teaching.repo.mapper.ClassCourseScheduleMapper;
import ${package}.infrastructure.teaching.repo.mapper.CourseMapper;
import ${package}.infrastructure.teaching.repo.mapper.SchoolClassMapper;
import ${package}.infrastructure.teaching.repo.po.ClassCourseSchedulePO;
import ${package}.infrastructure.teaching.repo.po.CoursePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

@Repository("schoolClassRepository")
@RequiredArgsConstructor
public class SchoolClassRepositoryImpl implements SchoolClassRepository {
    private final SchoolClassMapper schoolClassMapper;
    private final CourseMapper courseMapper;
    private final ClassCourseScheduleMapper scheduleMapper;
    private final SchoolClassPOConverter schoolClassConverter;
    private final CoursePOConverter courseConverter;
    private final LongIdGenerator idGenerator;

    @Override
    public SchoolClass save(SchoolClass schoolClass) {
        var po = schoolClassConverter.toPO(schoolClass);
        int affected = schoolClassMapper.selectById(po.getId()) == null
                ? schoolClassMapper.insert(po)
                : schoolClassMapper.updateById(po);
        requireAffected(affected, "school class");
        return schoolClassConverter.toDomain(po);
    }

    @Override
    public Optional<SchoolClassAggregate> findAggregateById(SchoolClassId schoolClassId) {
        return Optional.ofNullable(schoolClassMapper.selectById(schoolClassId.value()))
                .map(schoolClassPO -> {
            SchoolClassAggregate aggregate = new SchoolClassAggregate(
                    schoolClassConverter.toDomain(schoolClassPO));
            scheduleMapper.findBySchoolClassIdOrderByStartsAt(schoolClassId.value())
                    .forEach(schedulePO -> restoreSchedule(aggregate, schedulePO));
            return aggregate;
        });
    }

    @Override
    @Transactional
    public void saveAggregate(SchoolClassAggregate aggregate) {
        save(aggregate.schoolClass());
        aggregate.schedules().forEach(schedule -> {
            CoursePO coursePO = courseMapper.findByCourseCode(schedule.courseCode().value());
            if (coursePO == null) {
                throw new IllegalStateException("scheduled course not found");
            }
            Course course = courseConverter.toDomain(coursePO);
            requireAffected(scheduleMapper.insertSchedule(new ClassCourseSchedulePO(
                    idGenerator.nextLongId(),
                    aggregate.schoolClass().id().value(),
                    course.id(),
                    schedule.startsAt(),
                    schedule.endsAt(),
                    Instant.now())), "class course schedule");
        });
    }

    private void restoreSchedule(
            SchoolClassAggregate aggregate, ClassCourseSchedulePO schedulePO) {
        CoursePO coursePO = courseMapper.selectById(schedulePO.getCourseId());
        if (coursePO == null) {
            throw new IllegalStateException("scheduled course not found");
        }
        Course course = courseConverter.toDomain(coursePO);
        aggregate.schedule(course, new CourseSchedule(
                course.code(), schedulePO.getStartsAt(), schedulePO.getEndsAt()));
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " persistence affected " + affected
                    + " rows");
        }
    }
}
