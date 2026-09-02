package top.egon.cola.archetype.source.webopen.application.teaching;

import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.application.teaching.command.CreateGradeCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.impl.GradeManageImpl;
import top.egon.cola.archetype.source.webopen.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.webopen.domain.client.CommandIdempotencyPort;
import top.egon.cola.archetype.source.webopen.domain.client.OrganizationEventPublisher;
import top.egon.cola.archetype.source.webopen.domain.teaching.client.GradeCachePort;
import top.egon.cola.archetype.source.webopen.domain.teaching.service.GradeDomainService;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.GradeCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeManageImplTest {
    @Mock GradeDomainService<?> gradeDomainService;
    @Mock GradeCachePort gradeCache;
    @Mock CommandIdempotencyPort idempotency;
    @Mock OrganizationEventPublisher eventPublisher;

    @AfterEach void clearContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void rejectsDuplicateGradeCode() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
                "teacher-1", Set.of("TEACHING_ADMIN"), "trace-1"));
        when(gradeDomainService.existsByCode(GradeCode.create("GRADE_ONE"))).thenReturn(true);
        when(idempotency.claim("create-grade", "req-1")).thenReturn(true);
        GradeManageImpl manage = new GradeManageImpl(gradeDomainService,
                new TeachingApplicationValidator(), gradeCache, idempotency, eventPublisher,
                () -> 2001L);

        assertThrows(OrganizationApplicationException.class, () -> manage.createGrade(
                new CreateGradeCommand("req-1", "grade_one", "Grade One")));
    }
}
