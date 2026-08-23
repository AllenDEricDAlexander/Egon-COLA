#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo;

import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.enums.CourseScheduleStatus;
import ${package}.domain.course.vos.CourseId;
import ${package}.infrastructure.course.repo.converter.CourseScheduleConverter;
import ${package}.infrastructure.course.repo.impl.CourseScheduleRepositoryImpl;
import ${package}.infrastructure.course.repo.mapper.CourseScheduleMapper;
import ${package}.infrastructure.course.repo.po.CourseSchedulePo;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseScheduleRepositoryTest {

    @Test
    void shouldInsertAssignedIdWithoutJpaMergeLookup() {
        CourseScheduleMapper mapper = mock(CourseScheduleMapper.class);
        CourseScheduleRepositoryImpl repository = new CourseScheduleRepositoryImpl(
                mapper, new CourseScheduleConverter());
        CourseSchedule schedule = schedule();
        when(mapper.selectByCourseIdAndId(1001L, 3001L)).thenReturn(null);
        when(mapper.insert(any(CourseSchedulePo.class))).thenReturn(1);

        CourseSchedule result = repository.save(schedule);

        assertThat(result.getId()).isEqualTo(3001L);
        verify(mapper).insert(any(CourseSchedulePo.class));
    }

    @Test
    void shouldUseCourseAndClassWindowForOverlapQuery() {
        CourseScheduleMapper mapper = mock(CourseScheduleMapper.class);
        CourseSchedulePo row = new CourseSchedulePo(
                3001L, 1001L, 2001L, Instant.EPOCH,
                Instant.EPOCH.plusSeconds(60), "SCHEDULED", Instant.EPOCH, Instant.EPOCH);
        when(mapper.selectOverlapping(
                1001L, 2001L, Instant.EPOCH.plusSeconds(120), Instant.EPOCH))
                .thenReturn(List.of(row));
        CourseScheduleRepositoryImpl repository = new CourseScheduleRepositoryImpl(
                mapper, new CourseScheduleConverter());

        List<CourseSchedule> result = repository.findOverlapping(
                new CourseId(1001L), 2001L,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(120));

        assertThat(result).extracting(CourseSchedule::getId).containsExactly(3001L);
        verify(mapper).selectOverlapping(
                1001L, 2001L, Instant.EPOCH.plusSeconds(120), Instant.EPOCH);
    }

    private static CourseSchedule schedule() {
        return new CourseSchedule(
                3001L, new CourseId(1001L), 2001L,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), CourseScheduleStatus.SCHEDULED);
    }
}
