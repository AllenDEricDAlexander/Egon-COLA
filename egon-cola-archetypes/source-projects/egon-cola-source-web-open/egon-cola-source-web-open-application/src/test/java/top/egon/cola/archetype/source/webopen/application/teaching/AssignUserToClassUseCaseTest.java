package top.egon.cola.archetype.source.webopen.application.teaching;

import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.impl.SchoolClassManageImpl;
import top.egon.cola.archetype.source.webopen.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.webopen.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.webopen.domain.service.OrganizationEventService;
import top.egon.cola.archetype.source.webopen.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.webopen.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.convertor.SchoolClassConverter;
import top.egon.cola.archetype.source.webopen.domain.teaching.service.SchoolClassDomainService;
import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.webopen.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import org.mapstruct.factory.Mappers;
import top.egon.cola.component.common.core.validation.ValidationUtils;

@ExtendWith(MockitoExtension.class)
class AssignUserToClassUseCaseTest {
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
    void rejectsDuplicateMembershipWithoutWriting() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
                "teacher-1", Set.of("TEACHING_ADMIN"), "trace-1"));
        User user = new User(new UserId(3001L), "Mario", "mario@example.com", UserStatus.ACTIVE);
        SchoolClass schoolClass = new SchoolClass(new SchoolClassId(2001L), "Class A", 1001L,
                GradeCode.create("GRADE_ONE"), "Grade One", SchoolClassStatus.ACTIVE, List.of());
        when(userDomainService.findById(new UserId(3001L))).thenReturn(Optional.of(user));
        when(schoolClassDomainService.findByGradeIdAndId(1001L, new SchoolClassId(2001L)))
                .thenReturn(Optional.of(schoolClass));
        when(schoolClassDomainService.hasUser(1001L, new SchoolClassId(2001L), new UserId(3001L)))
                .thenReturn(true);
        when(idempotency.claim("assign-user-to-school-class", "req-2")).thenReturn(true);
        SchoolClassManageImpl manage = new SchoolClassManageImpl(schoolClassDomainService, userDomainService, teachingValidator(),
                idempotency, eventPublisher, Mappers.getMapper(SchoolClassConverter.class));

        assertThrows(OrganizationApplicationException.class, () -> manage.assignUser(
                new AssignUserToClassCommand("req-2", 1001L, 2001L, 3001L)));
    }
}
