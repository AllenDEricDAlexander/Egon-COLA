package top.egon.cola.archetype.source.web.starter;

import top.egon.cola.archetype.source.web.application.teaching.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.command.CreateGradeCommand;
import top.egon.cola.archetype.source.web.application.teaching.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.web.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.web.infrastructure.cache.InMemoryCommandIdempotencyAdapter;
import top.egon.cola.archetype.source.web.infrastructure.teaching.cache.InMemorySchoolClassCache;
import top.egon.cola.archetype.source.web.infrastructure.mq.LocalOrganizationEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.MDC;
import top.egon.cola.component.common.id.generator.IdGenerator;

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
    @Autowired private IdGenerator idGenerator;

    @AfterEach
    void clearContext() {
        OrganizationRequestContextHolder.clear();
        MDC.remove("tenantId");
        MDC.remove("userId");
    }

    @Test
    void domainRejectionRollsBackEverySideEffect() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
                "admin-1", Set.of("TEACHING_ADMIN"), "rollback-test"));
        MDC.put("tenantId", "1");
        MDC.put("userId", "admin-1");
        String suffix = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String gradeCode = "ROLLBACK_" + suffix;
        var grade = gradeManage.createGrade(
                new CreateGradeCommand("grade-" + suffix, gradeCode, "Rollback Grade"));
        var schoolClass = schoolClassManage.createSchoolClass(
                new CreateSchoolClassCommand("class-" + suffix, "Rollback Class", gradeCode));
        Long disabledUserId = 9001L;
        jdbcTemplate.update(
                "insert into users(id, name, email, status, create_time, tenant_id)"
                        + " values (?, ?, ?, ?, ?, ?)",
                disabledUserId, "Disabled User", disabledUserId + "@example.com", "DISABLED",
                Timestamp.from(Instant.now()), 1L);

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
                        + " where tenant_id = ? and grade_id = ? and user_id = ? and school_class_id = ?",
                Integer.class, 1L, grade.id(), disabledUserId, schoolClass.id())).isZero();
    }
}
