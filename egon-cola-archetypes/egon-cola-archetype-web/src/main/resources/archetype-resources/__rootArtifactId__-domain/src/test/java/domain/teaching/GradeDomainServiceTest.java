package ${package}.domain.teaching;

import ${package}.domain.entities.teaching.Grade;
import ${package}.domain.enums.teaching.GradeStatus;
import ${package}.domain.service.teaching.GradeDomainService;
import ${package}.domain.service.teaching.impl.GradeDomainServiceImpl;
import ${package}.domain.vos.teaching.GradeCode;
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
