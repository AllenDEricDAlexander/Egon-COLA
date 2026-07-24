package ${package}.starter;

import ${package}.application.teaching.command.AssignUserToClassCommand;
import ${package}.application.teaching.command.CreateGradeCommand;
import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.teaching.manage.GradeManage;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.infrastructure.cache.InMemoryCommandIdempotencyAdapter;
import ${package}.infrastructure.teaching.cache.InMemorySchoolClassCache;
import ${package}.infrastructure.mq.LocalOrganizationEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = OrganizationApplication.class,
        properties = "spring.profiles.active=test")
class OrganizationRollbackTest {

    @Autowired private GradeManage gradeManage;
    @Autowired private SchoolClassManage schoolClassManage;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private LocalOrganizationEventPublisher localPublisher;
    @Autowired private InMemorySchoolClassCache schoolClassCache;
    @Autowired private InMemoryCommandIdempotencyAdapter idempotency;

    @AfterEach
    void clearContext() {
        OrganizationRequestContextHolder.clear();
    }

    @Test
    void domainRejectionRollsBackEverySideEffect() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
                "admin-1", Set.of("TEACHING_ADMIN"), "rollback-test"));
        String suffix = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String gradeCode = "ROLLBACK_" + suffix;
        var grade = gradeManage.createGrade(
                new CreateGradeCommand("grade-" + suffix, gradeCode, "Rollback Grade"));
        var schoolClass = schoolClassManage.createSchoolClass(
                new CreateSchoolClassCommand("class-" + suffix, "Rollback Class", gradeCode));
        String disabledUserId = "disabled-" + UUID.randomUUID();
        jdbcTemplate.update(
                "insert into users(id, name, email, status, created_at) values (?, ?, ?, ?, ?)",
                disabledUserId, "Disabled User", disabledUserId + "@example.com", "DISABLED",
                Timestamp.from(Instant.now()));

        localPublisher.clear();
        schoolClassCache.clearObservations();
        idempotency.clear();
        AssignUserToClassCommand command = new AssignUserToClassCommand(
                "rollback-1", grade.id(), schoolClass.id(), disabledUserId);

        assertThatThrownBy(() -> schoolClassManage.assignUser(command))
                .isInstanceOf(OrganizationApplicationException.class);
        assertThat(localPublisher.events()).isEmpty();
        assertThat(schoolClassCache.evictedKeys()).isEmpty();
        assertThat(idempotency.contains("assign-user-to-school-class", "rollback-1")).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from school_class_users"
                        + " where grade_id = ? and user_id = ? and school_class_id = ?",
                Integer.class, grade.id(), disabledUserId, schoolClass.id())).isZero();
    }
}
