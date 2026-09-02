package top.egon.cola.archetype.source.webopen.domain.teaching;

import top.egon.cola.archetype.source.webopen.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.webopen.domain.teaching.enums.GradeStatus;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.GradeCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradeDomainServiceTest {
    @Test
    void keepsLongIdentityAndNormalizedValueObjects() {
        Grade grade = new Grade(1001L, GradeCode.create("grade_one"), " Grade One ", GradeStatus.ACTIVE);

        assertEquals(1001L, grade.id());
        assertEquals(GradeCode.create("GRADE_ONE"), grade.code());
        assertEquals("Grade One", grade.name());
        assertEquals(GradeStatus.ACTIVE, grade.status());
    }
}
