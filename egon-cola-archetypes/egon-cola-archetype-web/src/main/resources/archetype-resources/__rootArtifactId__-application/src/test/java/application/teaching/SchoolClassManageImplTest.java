package ${package}.application.teaching;

import ${package}.application.command.teaching.CreateSchoolClassCommand;
import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.manage.teaching.impl.SchoolClassManageImpl;
import ${package}.application.validators.teaching.TeachingApplicationValidator;
import ${package}.domain.entities.teaching.Grade;
import ${package}.domain.enums.teaching.GradeStatus;
import ${package}.domain.repos.teaching.GradeRepository;
import ${package}.domain.repos.teaching.SchoolClassRepository;
import ${package}.domain.repos.user.UserRepository;
import ${package}.domain.service.teaching.SchoolClassDomainService;
import ${package}.domain.vos.teaching.GradeCode;
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
    @Mock GradeRepository gradeRepository;
    @Mock SchoolClassRepository schoolClassRepository;
    @Mock UserRepository userRepository;

    @AfterEach void clearContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void rejectsDuplicateClassNameWithinGradeIgnoringCase() {
        OrganizationRequestContextHolder.set(new OrganizationRequestContext(
            "teacher-1", Set.of("TEACHING_ADMIN"), "trace-1"));
        Grade grade = new Grade("grade-1", GradeCode.create("GRADE_ONE"), "Grade One", GradeStatus.ACTIVE);
        when(gradeRepository.findByCode(GradeCode.create("GRADE_ONE"))).thenReturn(Optional.of(grade));
        when(schoolClassRepository.existsByGradeIdAndNameIgnoreCase("grade-1", "Class A")).thenReturn(true);
        SchoolClassManageImpl manage = new SchoolClassManageImpl(
            schoolClassRepository, gradeRepository, userRepository, new SchoolClassDomainService(),
            new TeachingApplicationValidator());

        assertThrows(OrganizationApplicationException.class, () -> manage.createSchoolClass(
            new CreateSchoolClassCommand("req-1", "Class A", "grade_one")));
    }
}
