package ${package}.application.teaching;

import ${package}.application.command.teaching.AssignUserToClassCommand;
import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.manage.teaching.impl.SchoolClassManageImpl;
import ${package}.application.validators.teaching.TeachingApplicationValidator;
import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.entities.user.User;
import ${package}.domain.enums.teaching.SchoolClassStatus;
import ${package}.domain.enums.user.UserStatus;
import ${package}.domain.repos.teaching.GradeRepository;
import ${package}.domain.repos.teaching.SchoolClassRepository;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.client.teaching.SchoolClassCachePort;
import ${package}.domain.service.teaching.SchoolClassDomainService;
import ${package}.domain.vos.teaching.GradeCode;
import ${package}.domain.vos.teaching.SchoolClassId;
import ${package}.domain.vos.user.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
        when(userRepository.findById(new UserId("u-1"))).thenReturn(Optional.of(activeUser("u-1")));
        when(schoolClassRepository.findById(new SchoolClassId("c-1"))).thenReturn(Optional.of(activeClass("c-1")));
        when(idempotency.claim("assign-user-to-school-class", "req-1")).thenReturn(true);
        SchoolClassManageImpl manage = manage();

        manage.assignUser(new AssignUserToClassCommand("req-1", "u-1", "c-1"));

        verify(schoolClassRepository).addUser(new SchoolClassId("c-1"), new UserId("u-1"));
    }

    @Test
    void rejectsDuplicateMembershipWithoutWriting() {
        setContext();
        when(userRepository.findById(new UserId("u-1"))).thenReturn(Optional.of(activeUser("u-1")));
        when(schoolClassRepository.findById(new SchoolClassId("c-1"))).thenReturn(Optional.of(activeClass("c-1")));
        when(schoolClassRepository.hasUser(new SchoolClassId("c-1"), new UserId("u-1"))).thenReturn(true);
        when(idempotency.claim("assign-user-to-school-class", "req-2")).thenReturn(true);

        assertThrows(OrganizationApplicationException.class, () -> manage().assignUser(
            new AssignUserToClassCommand("req-2", "u-1", "c-1")));
        verify(schoolClassRepository, never()).addUser(any(), any());
    }

    private SchoolClassManageImpl manage() {
        return new SchoolClassManageImpl(schoolClassRepository, gradeRepository, userRepository,
            new SchoolClassDomainService(), new TeachingApplicationValidator(), schoolClassCache, idempotency,
            eventPublisher);
    }

    private static User activeUser(String id) {
        return new User(new UserId(id), "Mario", "mario@example.com", UserStatus.ACTIVE);
    }

    private static SchoolClass activeClass(String id) {
        return new SchoolClass(new SchoolClassId(id), "Class A", "grade-1",
            GradeCode.create("GRADE_ONE"), "Grade One", SchoolClassStatus.ACTIVE, List.of());
    }

    private static void setContext() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
            "teacher-1", Set.of("TEACHING_ADMIN"), "trace-1"));
    }
}
