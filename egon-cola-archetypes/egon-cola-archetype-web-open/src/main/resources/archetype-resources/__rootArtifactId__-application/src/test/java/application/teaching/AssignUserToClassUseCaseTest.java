package ${package}.application.teaching;

import ${package}.application.teaching.command.AssignUserToClassCommand;
import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.teaching.manage.impl.SchoolClassManageImpl;
import ${package}.application.teaching.validators.TeachingApplicationValidator;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.user.entities.User;
import ${package}.domain.teaching.enums.SchoolClassStatus;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.teaching.repos.GradeRepository;
import ${package}.domain.teaching.repos.SchoolClassRepository;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.teaching.client.SchoolClassCachePort;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignUserToClassUseCaseTest {
    @Mock SchoolClassRepository schoolClassRepository;
    @Mock GradeRepository gradeRepository;
    @Mock UserRepository userRepository;
    @Mock SchoolClassCachePort schoolClassCache;
    @Mock CommandIdempotencyPort idempotency;
    @Mock OrganizationEventPublisher eventPublisher;

    @AfterEach void clearContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void assignsActiveUserToActiveSchoolClassInOneTransaction() {
        setContext();
        when(userRepository.findById(new UserId(1001L))).thenReturn(Optional.of(activeUser(1001L)));
        when(schoolClassRepository.findByGradeIdAndId(2001L, new SchoolClassId(3001L)))
                .thenReturn(Optional.of(activeClass(3001L)));
        when(idempotency.claim("assign-user-to-school-class", "req-1")).thenReturn(true);
        SchoolClassManageImpl manage = manage();

        manage.assignUser(new AssignUserToClassCommand("req-1", 2001L, 3001L, 1001L));

        verify(schoolClassRepository)
                .addUser(2001L, new SchoolClassId(3001L), new UserId(1001L));
    }

    @Test
    void rejectsDuplicateMembershipWithoutWriting() {
        setContext();
        when(userRepository.findById(new UserId(1001L))).thenReturn(Optional.of(activeUser(1001L)));
        when(schoolClassRepository.findByGradeIdAndId(2001L, new SchoolClassId(3001L)))
                .thenReturn(Optional.of(activeClass(3001L)));
        when(schoolClassRepository.hasUser(
                2001L, new SchoolClassId(3001L), new UserId(1001L))).thenReturn(true);
        when(idempotency.claim("assign-user-to-school-class", "req-2")).thenReturn(true);

        assertThrows(OrganizationApplicationException.class, () -> manage().assignUser(
            new AssignUserToClassCommand("req-2", 2001L, 3001L, 1001L)));
        verify(schoolClassRepository, never()).addUser(anyLong(), any(), any());
    }

    private SchoolClassManageImpl manage() {
        return new SchoolClassManageImpl(schoolClassRepository, gradeRepository, userRepository,
            new SchoolClassDomainService(), new TeachingApplicationValidator(), schoolClassCache, idempotency,
            eventPublisher, (LongIdGenerator) () -> 4001L);
    }

    private static User activeUser(Long id) {
        return new User(new UserId(id), "Mario", "mario@example.com", UserStatus.ACTIVE);
    }

    private static SchoolClass activeClass(Long id) {
        return new SchoolClass(new SchoolClassId(id), "Class A", 2001L,
            GradeCode.create("GRADE_ONE"), "Grade One", SchoolClassStatus.ACTIVE, List.of());
    }

    private static void setContext() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
            "teacher-1", Set.of("TEACHING_ADMIN"), "trace-1"));
    }
}
