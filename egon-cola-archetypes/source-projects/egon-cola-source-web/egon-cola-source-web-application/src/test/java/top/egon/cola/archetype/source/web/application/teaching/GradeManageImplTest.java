package top.egon.cola.archetype.source.web.application.teaching;

import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.common.exception.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.application.teaching.pojo.command.CreateGradeCommand;
import top.egon.cola.archetype.source.web.application.teaching.manage.impl.GradeManageImpl;
import top.egon.cola.archetype.source.web.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.web.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.web.domain.service.OrganizationEventService;
import top.egon.cola.archetype.source.web.application.teaching.pojo.convertor.GradeConverter;
import top.egon.cola.archetype.source.web.domain.teaching.service.GradeDomainService;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import org.mapstruct.factory.Mappers;
import top.egon.cola.component.common.core.validation.ValidationUtils;

@ExtendWith(MockitoExtension.class)
class GradeManageImplTest {
    @Mock ValidationUtils validationUtils;

    private TeachingApplicationValidator teachingValidator() {
        return new TeachingApplicationValidator(validationUtils);
    }

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    @Mock GradeDomainService gradeDomainService;
    @Mock CommandIdempotencyService idempotency;
    @Mock OrganizationEventService eventPublisher;

    @AfterEach void clearContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void rejectsDuplicateGradeCode() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
                "teacher-1", Set.of("TEACHING_ADMIN"), "trace-1"));
        when(gradeDomainService.existsByCode(GradeCode.create("GRADE_ONE"))).thenReturn(true);
        when(idempotency.claim("create-grade", "req-1")).thenReturn(true);
        GradeManageImpl manage = new GradeManageImpl(gradeDomainService, teachingValidator(), idempotency, eventPublisher,
                Mappers.getMapper(GradeConverter.class));

        assertThrows(OrganizationApplicationException.class, () -> manage.createGrade(
                new CreateGradeCommand("req-1", "grade_one", "Grade One")));
    }
}
