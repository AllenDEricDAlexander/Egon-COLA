package ${package}.domain.teaching;

import ${package}.domain.teaching.entities.Grade;
import ${package}.domain.teaching.enums.GradeStatus;
import ${package}.domain.teaching.service.GradeDomainService;
import ${package}.domain.teaching.service.impl.GradeDomainServiceImpl;
import ${package}.domain.teaching.vos.GradeCode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradeDomainServiceTest {
    private final GradeDomainService service = new GradeDomainServiceImpl();

    @Test
    void createsNormalizedActiveGrade() {
        Grade grade = service.create("grade-" + UUID.randomUUID(), "grade_one", " Grade One ");

        assertEquals(GradeCode.create("GRADE_ONE"), grade.code());
        assertEquals("Grade One", grade.name());
        assertEquals(GradeStatus.ACTIVE, grade.status());
    }
}
