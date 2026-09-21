package top.egon.cola.archetype.source.web.application.teaching;

import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.common.exception.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.manage.impl.SchoolClassManageImpl;
import top.egon.cola.archetype.source.web.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.web.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.web.domain.service.OrganizationEventService;
import top.egon.cola.archetype.source.web.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.web.domain.teaching.enums.GradeStatus;
import top.egon.cola.archetype.source.web.application.teaching.pojo.convertor.SchoolClassConverter;
import top.egon.cola.archetype.source.web.domain.teaching.service.SchoolClassDomainService;
import top.egon.cola.archetype.source.web.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import org.mapstruct.factory.Mappers;
import top.egon.cola.component.common.core.validation.ValidationUtils;

@ExtendWith(MockitoExtension.class)
class SchoolClassManageImplTest {
    @Mock ValidationUtils validationUtils;

    private TeachingApplicationValidator teachingValidator() {
        return new TeachingApplicationValidator(validationUtils);
    }

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    @Mock SchoolClassDomainService schoolClassDomainService;
    @Mock UserDomainService userDomainService;
    @Mock CommandIdempotencyService idempotency;
    @Mock OrganizationEventService eventPublisher;

    @AfterEach void clearContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void rejectsDuplicateClassNameWithinGradeIgnoringCase() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
                "teacher-1", Set.of("TEACHING_ADMIN"), "trace-1"));
        Grade grade = new Grade(1001L, GradeCode.create("GRADE_ONE"), "Grade One", GradeStatus.ACTIVE);
        when(schoolClassDomainService.findGradeByCode(GradeCode.create("GRADE_ONE")))
                .thenReturn(Optional.of(grade));
        when(schoolClassDomainService.existsByGradeIdAndNameIgnoreCase(1001L, "Class A"))
                .thenReturn(true);
        when(idempotency.claim("create-school-class", "req-1")).thenReturn(true);
        SchoolClassManageImpl manage = new SchoolClassManageImpl(schoolClassDomainService, userDomainService, teachingValidator(),
                idempotency, eventPublisher, Mappers.getMapper(SchoolClassConverter.class));

        assertThrows(OrganizationApplicationException.class, () -> manage.createSchoolClass(
                new CreateSchoolClassCommand("req-1", "Class A", "grade_one")));
    }
}
