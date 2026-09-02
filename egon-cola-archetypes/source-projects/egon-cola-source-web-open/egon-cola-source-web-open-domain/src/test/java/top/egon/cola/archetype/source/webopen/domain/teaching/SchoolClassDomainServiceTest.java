package top.egon.cola.archetype.source.webopen.domain.teaching;

import top.egon.cola.archetype.source.webopen.domain.teaching.entities.Grade;
import top.egon.cola.archetype.source.webopen.domain.teaching.entities.SchoolClass;
import top.egon.cola.archetype.source.webopen.domain.teaching.enums.GradeStatus;
import top.egon.cola.archetype.source.webopen.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.SchoolClassId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchoolClassDomainServiceTest {
    @Test
    void keepsLongGradeAndClassIdentity() {
        Grade grade = new Grade(1001L, GradeCode.create("GRADE_ONE"), "Grade One", GradeStatus.ACTIVE);
        SchoolClass schoolClass = new SchoolClass(new SchoolClassId(2001L), "Class A", grade.id(),
                grade.code(), grade.name(), SchoolClassStatus.ACTIVE, List.of());

        assertEquals(2001L, schoolClass.id().value());
        assertEquals(1001L, schoolClass.gradeId());
        assertEquals("Class A", schoolClass.name());
    }
}
