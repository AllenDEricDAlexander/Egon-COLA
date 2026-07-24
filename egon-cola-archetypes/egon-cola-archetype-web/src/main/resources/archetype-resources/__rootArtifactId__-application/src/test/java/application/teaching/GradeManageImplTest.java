package ${package}.application.teaching;

import ${package}.application.teaching.command.CreateGradeCommand;
import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.teaching.manage.impl.GradeManageImpl;
import ${package}.application.teaching.validators.TeachingApplicationValidator;
import ${package}.domain.teaching.repos.GradeRepository;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.teaching.client.GradeCachePort;
import ${package}.domain.teaching.service.impl.GradeDomainServiceImpl;
import ${package}.domain.teaching.vos.GradeCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeManageImplTest {
    @Mock GradeRepository gradeRepository;
    @Mock GradeCachePort gradeCache;
    @Mock CommandIdempotencyPort idempotency;
    @Mock OrganizationEventPublisher eventPublisher;

    @AfterEach void clearContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void rejectsDuplicateGradeCode() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
            "teacher-1", Set.of("TEACHING_ADMIN"), "trace-1"));
        when(gradeRepository.existsByCode(GradeCode.create("GRADE_ONE"))).thenReturn(true);
        when(idempotency.claim("create-grade", "req-1")).thenReturn(true);
        GradeManageImpl manage = new GradeManageImpl(
            gradeRepository, new GradeDomainServiceImpl(), new TeachingApplicationValidator(),
            gradeCache, idempotency, eventPublisher, new UuidV7Generator());

        assertThrows(OrganizationApplicationException.class, () -> manage.createGrade(
            new CreateGradeCommand("req-1", "grade_one", "Grade One")));
    }
}
