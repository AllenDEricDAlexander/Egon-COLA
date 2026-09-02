package top.egon.cola.archetype.source.web.application.teaching;

import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.application.teaching.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.manage.impl.SchoolClassManageImpl;
import top.egon.cola.archetype.source.web.application.teaching.validators.TeachingApplicationValidator;
import top.egon.cola.archetype.source.web.domain.client.CommandIdempotencyPort;
import top.egon.cola.archetype.source.web.domain.client.OrganizationEventPublisher;
import top.egon.cola.archetype.source.web.domain.teaching.client.SchoolClassCachePort;
import top.egon.cola.archetype.source.web.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.web.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.web.domain.teaching.service.SchoolClassDomainService;
import top.egon.cola.archetype.source.web.domain.user.entities.User;
import top.egon.cola.archetype.source.web.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.web.domain.user.service.UserDomainService;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.web.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignUserToClassUseCaseTest {
    @Mock SchoolClassDomainService<?> schoolClassDomainService;
    @Mock UserDomainService<?> userDomainService;
    @Mock SchoolClassCachePort schoolClassCache;
    @Mock CommandIdempotencyPort idempotency;
    @Mock OrganizationEventPublisher eventPublisher;

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
        SchoolClassManageImpl manage = new SchoolClassManageImpl(schoolClassDomainService, userDomainService,
                new TeachingApplicationValidator(), schoolClassCache, idempotency, eventPublisher, () -> 2001L);

        assertThrows(OrganizationApplicationException.class, () -> manage.assignUser(
                new AssignUserToClassCommand("req-2", 1001L, 2001L, 3001L)));
    }
}
