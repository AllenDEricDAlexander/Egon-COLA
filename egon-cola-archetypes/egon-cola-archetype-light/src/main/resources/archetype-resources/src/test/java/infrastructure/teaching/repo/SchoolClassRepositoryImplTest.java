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
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties =
        "spring.jpa.properties.hibernate.session_factory.statement_inspector="
                + "${package}.infrastructure.teaching.repo.SqlCaptureStatementInspector")
@ActiveProfiles({"test", "jpa-test"})
@ContextConfiguration(classes = JpaTestApplication.class)
@Import({
        SchoolClassRepositoryImpl.class,
        CourseRepositoryImpl.class,
        SchoolClassPOConverter.class,
        CoursePOConverter.class,
        CoursePOMapperImpl.class,
        UuidV7Generator.class
})
class SchoolClassRepositoryImplTest {
    private static final String SCHOOL_CLASS_ID = "018f5f9c-4f6a-7c2b-8a1d-123456789ab3";
    private static final String COURSE_ID = "018f5f9c-4f6a-7c2b-8a1d-123456789ab2";

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
        SqlCaptureStatementInspector.clear();
        schoolClassRepository.saveAggregate(aggregate);
        assertThat(SqlCaptureStatementInspector.statements())
                .noneMatch(sql -> isIdOnlyLookup(sql, "class_course_schedules"));
        entityManager.flush();
        entityManager.clear();

        SchoolClassAggregate restored = schoolClassRepository
                .findAggregateById(new SchoolClassId(SCHOOL_CLASS_ID))
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
        assertThrows(PersistenceException.class, () -> {
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
        assertEquals(COURSE_ID, mapped.getId());
        assertEquals("math", mapped.getCourseCode());
    }

    private SchoolClass schoolClass() {
        return new SchoolClass(
                new SchoolClassId(SCHOOL_CLASS_ID), "Class One", new Semester("2026-FALL"),
                SchoolClassStatus.ACTIVE);
    }

    private Course course() {
        return new Course(COURSE_ID, new CourseCode("math"), "Mathematics", CourseStatus.ACTIVE);
    }

    private CourseSchedule schedule() {
        return new CourseSchedule(
                new CourseCode("math"),
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 1, 10, 0));
    }

    private static boolean isIdOnlyLookup(String sql, String table) {
        String normalized = sql.toLowerCase(Locale.ROOT);
        return normalized.contains(" from " + table + " ")
                && normalized.contains(" where ")
                && normalized.matches(".*where [a-z0-9_]+\\.id=\\?.*")
                && !normalized.contains("school_class_id=?");
    }
}
