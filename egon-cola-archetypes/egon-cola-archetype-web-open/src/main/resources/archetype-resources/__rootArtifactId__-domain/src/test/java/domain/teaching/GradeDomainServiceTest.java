package ${package}.domain.teaching;

import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.enums.GradeStatus;
import ${package}.domain.teaching.service.GradeDomainService;
import ${package}.domain.teaching.service.impl.GradeDomainServiceImpl;
import ${package}.domain.teaching.vos.GradeCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradeDomainServiceTest {
    private final GradeDomainService service = new GradeDomainServiceImpl();

    @Test
    void createsNormalizedActiveGrade() {
        String gradeId = "019ba346-0000-7000-8000-000000000010";
        Grade grade = service.create(gradeId, "grade_one", " Grade One ");

        assertEquals(gradeId, grade.id());
        assertEquals(GradeCode.create("GRADE_ONE"), grade.code());
        assertEquals("Grade One", grade.name());
        assertEquals(GradeStatus.ACTIVE, grade.status());
    }
}
