#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.course.impl;

import ${package}.domain.entities.course.CourseSchedule;
import ${package}.domain.repos.course.CourseScheduleRepository;
import ${package}.domain.vos.course.CourseId;
import ${package}.infrastructure.repo.course.converter.CourseScheduleConverter;
import ${package}.infrastructure.repo.course.jpa.CourseScheduleJpaRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CourseScheduleRepositoryImpl implements CourseScheduleRepository {
    private final CourseScheduleJpaRepository repository;
    private final CourseScheduleConverter converter;

    @Override
    public CourseSchedule save(CourseSchedule schedule) {
        return converter.toDomain(repository.saveAndFlush(converter.toPo(schedule)));
    }

    @Override
    public List<CourseSchedule> findOverlapping(
            CourseId courseId, String classId, Instant startsAt, Instant endsAt) {
        return repository
                .findByCourseIdAndClassIdAndStartsAtLessThanAndEndsAtGreaterThan(
                        courseId.value(), classId, endsAt, startsAt)
                .stream().map(converter::toDomain).toList();
    }
}
