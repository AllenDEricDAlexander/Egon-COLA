#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo;

import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.enums.CourseScheduleStatus;
import ${package}.domain.course.vos.CourseId;
import ${package}.infrastructure.course.repo.converter.CourseScheduleConverter;
import ${package}.infrastructure.course.repo.impl.CourseScheduleRepositoryImpl;
import ${package}.infrastructure.course.repo.jpa.CourseScheduleJpaRepository;
import ${package}.infrastructure.course.repo.po.CourseSchedulePo;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseScheduleRepositoryTest {

    @Test
    void shouldRoundTripCourseSchedulePersistenceModel() {
        CourseSchedule schedule = new CourseSchedule(
                3001L, new CourseId(1001L), 2001L,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), CourseScheduleStatus.SCHEDULED);
        CourseScheduleConverter converter = new CourseScheduleConverter();

        CourseSchedule restored = converter.toDomain(converter.toPo(schedule));

        assertEquals(3001L, restored.getId());
        assertEquals(new CourseId(1001L), restored.getCourseId());
        assertEquals(CourseScheduleStatus.SCHEDULED, restored.getStatus());
    }

    @Test
    void shouldPersistAssignedIdWithoutJpaMergeLookup() {
        CourseScheduleJpaRepository jpaRepository = mock(CourseScheduleJpaRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        CourseScheduleRepositoryImpl repository = new CourseScheduleRepositoryImpl(
                jpaRepository, new CourseScheduleConverter(), entityManager);
        CourseSchedule schedule = new CourseSchedule(
                3001L, new CourseId(1001L), 2001L,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), CourseScheduleStatus.SCHEDULED);
        when(jpaRepository.findByCourseIdAndId(1001L, 3001L))
                .thenReturn(Optional.empty());

        repository.save(schedule);

        verify(entityManager).persist(any(CourseSchedulePo.class));
        verify(jpaRepository, never()).save(any(CourseSchedulePo.class));
    }
}
