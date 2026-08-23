#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo.impl;

import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.repos.CourseScheduleRepository;
import ${package}.domain.course.vos.CourseId;
import ${package}.infrastructure.course.repo.converter.CourseScheduleConverter;
import ${package}.infrastructure.course.repo.mapper.CourseScheduleMapper;
import ${package}.infrastructure.course.repo.po.CourseSchedulePo;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class CourseScheduleRepositoryImpl implements CourseScheduleRepository {

    private final CourseScheduleMapper mapper;
    private final CourseScheduleConverter converter;

    @Override
    @Transactional
    public CourseSchedule save(CourseSchedule schedule) {
        CourseSchedulePo existing = mapper.selectByCourseIdAndId(
                schedule.getCourseId().value(), schedule.getId());
        CourseSchedulePo po;
        int affected;
        if (existing == null) {
            po = converter.toPo(schedule);
            affected = mapper.insert(po);
        } else {
            po = converter.updatePo(schedule, existing);
            affected = mapper.updateById(po);
        }
        requireAffected(affected, "save course schedule");
        return converter.toDomain(po);
    }

    @Override
    public List<CourseSchedule> findOverlapping(
            CourseId courseId, long classId, Instant startsAt, Instant endsAt) {
        return mapper.selectOverlapping(courseId.value(), classId, endsAt, startsAt)
                .stream().map(converter::toDomain).toList();
    }

    private static void requireAffected(int affected, String operation) {
        if (affected != 1) {
            throw new IllegalStateException(operation + " affected " + affected + " rows");
        }
    }
}
