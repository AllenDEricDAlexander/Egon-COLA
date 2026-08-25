package ${package}.application.teaching;

import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.manage.impl.SchoolClassManageImpl;
import ${package}.application.teaching.validators.TeachingApplicationValidator;
import ${package}.domain.client.CommandIdempotencyPort;
import ${package}.domain.client.OrganizationEventPublisher;
import ${package}.domain.teaching.client.SchoolClassCachePort;
import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.enums.GradeStatus;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.teaching.vos.GradeCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolClassManageImplTest {
    @Mock SchoolClassDomainService<?> schoolClassDomainService;
    @Mock UserDomainService<?> userDomainService;
    @Mock SchoolClassCachePort schoolClassCache;
    @Mock CommandIdempotencyPort idempotency;
    @Mock OrganizationEventPublisher eventPublisher;

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
        SchoolClassManageImpl manage = new SchoolClassManageImpl(schoolClassDomainService, userDomainService,
                new TeachingApplicationValidator(), schoolClassCache, idempotency, eventPublisher, () -> 2001L);

        assertThrows(OrganizationApplicationException.class, () -> manage.createSchoolClass(
                new CreateSchoolClassCommand("req-1", "Class A", "grade_one")));
    }
}
