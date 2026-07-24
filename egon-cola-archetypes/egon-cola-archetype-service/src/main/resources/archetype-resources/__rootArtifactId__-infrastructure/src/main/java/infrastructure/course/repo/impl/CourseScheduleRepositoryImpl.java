#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo.impl;

import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.repos.CourseScheduleRepository;
import ${package}.domain.course.vos.CourseId;
import ${package}.infrastructure.course.repo.converter.CourseScheduleConverter;
import ${package}.infrastructure.course.repo.jpa.CourseScheduleJpaRepository;
import ${package}.infrastructure.course.repo.po.CourseSchedulePo;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class CourseScheduleRepositoryImpl implements CourseScheduleRepository {
    private final CourseScheduleJpaRepository repository;
    private final CourseScheduleConverter converter;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public CourseSchedule save(CourseSchedule schedule) {
        CourseSchedulePo po = repository.findByCourseIdAndId(
                        schedule.getCourseId().value(), schedule.getId())
                .map(existing -> converter.updatePo(schedule, existing))
                .orElseGet(() -> persist(converter.toPo(schedule)));
        repository.flush();
        return converter.toDomain(po);
    }

    @Override
    public List<CourseSchedule> findOverlapping(
            CourseId courseId, String classId, Instant startsAt, Instant endsAt) {
        return repository
                .findByCourseIdAndClassIdAndStartsAtLessThanAndEndsAtGreaterThan(
                        courseId.value(), classId, endsAt, startsAt)
                .stream().map(converter::toDomain).toList();
    }

    private CourseSchedulePo persist(CourseSchedulePo po) {
        entityManager.persist(po);
        return po;
    }
}
