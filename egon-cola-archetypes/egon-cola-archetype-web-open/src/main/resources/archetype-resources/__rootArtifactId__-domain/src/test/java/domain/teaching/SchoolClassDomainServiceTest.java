package ${package}.domain.teaching;

import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.enums.GradeStatus;
import ${package}.domain.exceptions.OrganizationDomainException;
import ${package}.domain.teaching.service.SchoolClassDomainService;
import ${package}.domain.teaching.vos.GradeCode;
import ${package}.domain.teaching.vos.SchoolClassId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchoolClassDomainServiceTest {
    private final SchoolClassDomainService service = new SchoolClassDomainService();

    @Test
    void createsClassForActiveGradeAndRejectsArchivedGrade() {
        Grade grade = new Grade(1001L, GradeCode.create("GRADE_ONE"), "Grade One", GradeStatus.ACTIVE);
        SchoolClass schoolClass = service.create(new SchoolClassId(2001L), " Class A ", grade);

        assertEquals(1001L, schoolClass.gradeId());
        assertEquals("Grade One", schoolClass.gradeName());
        assertThrows(OrganizationDomainException.class, () -> service.create(
            new SchoolClassId(2002L), "Class A",
            new Grade(1002L, GradeCode.create("ARCHIVED_GRADE"),
                "Archived Grade", GradeStatus.ARCHIVED)));
    }
}
