package ${package}.application.teaching;

import ${package}.application.command.teaching.CreateGradeCommand;
import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.manage.teaching.impl.GradeManageImpl;
import ${package}.application.validators.teaching.TeachingApplicationValidator;
import ${package}.domain.repos.teaching.GradeRepository;
import ${package}.domain.service.teaching.impl.GradeDomainServiceImpl;
import ${package}.domain.vos.teaching.GradeCode;
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
    @Mock GradeRepository gradeRepository;

    @AfterEach void clearContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void rejectsDuplicateGradeCode() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
            "teacher-1", Set.of("TEACHING_ADMIN"), "trace-1"));
        when(gradeRepository.existsByCode(GradeCode.create("GRADE_ONE"))).thenReturn(true);
        GradeManageImpl manage = new GradeManageImpl(
            gradeRepository, new GradeDomainServiceImpl(), new TeachingApplicationValidator());

        assertThrows(OrganizationApplicationException.class, () -> manage.createGrade(
            new CreateGradeCommand("req-1", "grade_one", "Grade One")));
    }
}
