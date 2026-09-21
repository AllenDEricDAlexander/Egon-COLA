package top.egon.cola.archetype.source.web.starter;

import top.egon.cola.archetype.source.web.application.teaching.pojo.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.pojo.command.CreateGradeCommand;
import top.egon.cola.archetype.source.web.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.common.exception.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.web.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.web.infrastructure.config.OrganizationLocalFallbackConfig;
import top.egon.cola.archetype.source.web.infrastructure.mq.MqMessageService;
import top.egon.cola.archetype.source.web.infrastructure.service.impl.InMemoryCommandIdempotencyServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.MDC;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = OrganizationApplication.class,
        properties = "spring.profiles.active=test")
class OrganizationRollbackTest extends top.egon.cola.archetype.source.web.support.PersistenceTestSupport {

    @Autowired private GradeManage gradeManage;
    @Autowired private SchoolClassManage schoolClassManage;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MqMessageService messageService;
    @Autowired private InMemoryCommandIdempotencyServiceImpl idempotency;

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

        // The broker-free profile records publications instead of sending, so a rolled back
        // transaction can be observed through the same MQ boundary.
        var localPublisher = (OrganizationLocalFallbackConfig.LocalMqMessageService) messageService;
        localPublisher.clear();
        idempotency.clear();
        AssignUserToClassCommand command = new AssignUserToClassCommand(
                "rollback-1", grade.id(), schoolClass.id(), disabledUserId);

        assertThatThrownBy(() -> schoolClassManage.assignUser(command))
                .isInstanceOf(OrganizationApplicationException.class);
        assertThat(localPublisher.publishedMessages()).isEmpty();
        assertThat(idempotency.contains("assign-user-to-school-class", "rollback-1")).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from school_class_users"
                        + " where tenant_id = ? and grade_id = ? and user_id = ? and school_class_id = ?",
                Integer.class, 1L, grade.id(), disabledUserId, schoolClass.id())).isZero();
    }
}
