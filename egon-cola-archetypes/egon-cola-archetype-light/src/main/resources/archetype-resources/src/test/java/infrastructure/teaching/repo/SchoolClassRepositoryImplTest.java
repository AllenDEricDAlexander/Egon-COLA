package ${package}.infrastructure.teaching.repo;

import ${package}.domain.teaching.aggregates.SchoolClassAggregate;
import ${package}.domain.teaching.entities.Course;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.CourseStatus;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.teaching.repos.CourseRepository;
import ${package}.domain.teaching.repos.SchoolClassRepository;
import ${package}.domain.teaching.vos.CourseCode;
import ${package}.domain.teaching.vos.CourseSchedule;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.teaching.vos.Semester;
import ${package}.infrastructure.JpaTestApplication;
import ${package}.infrastructure.teaching.repo.converter.CoursePOConverter;
import ${package}.infrastructure.teaching.repo.converter.CoursePOMapper;
import ${package}.infrastructure.teaching.repo.converter.CoursePOMapperImpl;
import ${package}.infrastructure.teaching.repo.converter.SchoolClassPOConverter;
import ${package}.infrastructure.teaching.repo.impl.CourseRepositoryImpl;
import ${package}.infrastructure.teaching.repo.impl.SchoolClassRepositoryImpl;
import ${package}.infrastructure.teaching.repo.po.CoursePO;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles({"test", "jpa-test"})
@ContextConfiguration(classes = JpaTestApplication.class)
@Import({
        SchoolClassRepositoryImpl.class,
        CourseRepositoryImpl.class,
        SchoolClassPOConverter.class,
        CoursePOConverter.class,
        CoursePOMapperImpl.class
})
class SchoolClassRepositoryImplTest {
    @Autowired SchoolClassRepository schoolClassRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired CoursePOMapper coursePOMapper;
    @Autowired EntityManager entityManager;

    @Test
    void reconstructs_scheduled_class_aggregate() {
        Course course = courseRepository.save(course());
        SchoolClass schoolClass = schoolClassRepository.save(schoolClass());
        SchoolClassAggregate aggregate = new SchoolClassAggregate(schoolClass);
        aggregate.schedule(course, schedule());
        schoolClassRepository.saveAggregate(aggregate);
        entityManager.flush();
        entityManager.clear();

        SchoolClassAggregate restored = schoolClassRepository
                .findAggregateById(new SchoolClassId("class-1"))
                .orElseThrow();

        assertEquals(1, restored.schedules().size());
        assertEquals(new CourseCode("math"), restored.schedules().getFirst().courseCode());
    }

    @Test
    void rejects_duplicate_schedule() {
        Course course = courseRepository.save(course());
        SchoolClass schoolClass = schoolClassRepository.save(schoolClass());
        SchoolClassAggregate aggregate = new SchoolClassAggregate(schoolClass);
        aggregate.schedule(course, schedule());
        schoolClassRepository.saveAggregate(aggregate);
        assertThrows(DataIntegrityViolationException.class, () -> {
            schoolClassRepository.saveAggregate(aggregate);
            entityManager.flush();
        });
    }

    @Test
    void updates_target_when_mapping_course() {
        CoursePO target = new CoursePO(
                "old", "old", "Old", "INACTIVE", Instant.EPOCH);

        CoursePO mapped = coursePOMapper.convert(course(), target);

        assertSame(target, mapped);
        assertEquals("course-math", mapped.getId());
        assertEquals("math", mapped.getCourseCode());
    }

    private SchoolClass schoolClass() {
        return new SchoolClass(
                new SchoolClassId("class-1"), "Class One", new Semester("2026-FALL"),
                SchoolClassStatus.ACTIVE);
    }

    private Course course() {
        return new Course("course-math", new CourseCode("math"), "Mathematics", CourseStatus.ACTIVE);
    }

    private CourseSchedule schedule() {
        return new CourseSchedule(
                new CourseCode("math"),
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 1, 10, 0));
    }
}
