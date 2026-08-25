#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo;

import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.converter.ExamConverter;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamRepositoryTest {
    @Test
    void shouldRoundTripExamPersistenceModel() {
        Exam exam = new Exam(new ExamId(4001L), new CourseId(1001L), "Midterm",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), ExamStatus.DRAFT);
        ExamConverter converter = Mappers.getMapper(ExamConverter.class);
        var target = converter.toTarget(exam);
        target.setId(exam.getId().value());
        Exam restored = converter.toSource(target);
        assertEquals(4001L, restored.getId().value());
        assertEquals(1001L, restored.getCourseId().value());
    }
}
