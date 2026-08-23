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
        Grade grade = new Grade("grade-1", GradeCode.create("GRADE_ONE"), "Grade One", GradeStatus.ACTIVE);
        SchoolClass schoolClass = service.create(new SchoolClassId("class-1"), " Class A ", grade);

        assertEquals("grade-1", schoolClass.gradeId());
        assertEquals("Grade One", schoolClass.gradeName());
        assertThrows(OrganizationDomainException.class, () -> service.create(
            new SchoolClassId("class-2"), "Class A",
            new Grade("grade-archived", GradeCode.create("ARCHIVED_GRADE"),
                "Archived Grade", GradeStatus.ARCHIVED)));
    }
}
