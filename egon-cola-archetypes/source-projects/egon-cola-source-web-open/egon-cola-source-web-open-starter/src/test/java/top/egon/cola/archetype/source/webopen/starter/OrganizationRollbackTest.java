package top.egon.cola.archetype.source.webopen.starter;

import top.egon.cola.archetype.source.webopen.application.teaching.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.command.CreateGradeCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.webopen.infrastructure.cache.InMemoryCommandIdempotencyAdapter;
import top.egon.cola.archetype.source.webopen.infrastructure.teaching.cache.InMemorySchoolClassCache;
import top.egon.cola.archetype.source.webopen.infrastructure.mq.LocalOrganizationEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import org.slf4j.MDC;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = OrganizationApplication.class,
        properties = "spring.profiles.active=test")
@ContextConfiguration(initializers = OrganizationManualSchemaTestSupport.Initializer.class)
class OrganizationRollbackTest {

    @Autowired private GradeManage gradeManage;
    @Autowired private SchoolClassManage schoolClassManage;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private LocalOrganizationEventPublisher localPublisher;
    @Autowired private InMemorySchoolClassCache schoolClassCache;
    @Autowired private InMemoryCommandIdempotencyAdapter idempotency;
    @Autowired private LongIdGenerator idGenerator;

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
        String suffix = Long.toString(System.nanoTime());
        String gradeCode = "ROLLBACK_" + suffix;
        var grade = gradeManage.createGrade(
                new CreateGradeCommand("grade-" + suffix, gradeCode, "Rollback Grade"));
        var schoolClass = schoolClassManage.createSchoolClass(
                new CreateSchoolClassCommand("class-" + suffix, "Rollback Class", gradeCode));
        Long disabledUserId = idGenerator.nextLongId();
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
                        + " where tenant_id = 1 and grade_id = ? and user_id = ? and school_class_id = ?",
                Integer.class, grade.id(), disabledUserId, schoolClass.id())).isZero();
    }
}
