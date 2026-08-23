#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo;

import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.converter.ExamConverter;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamRepositoryTest {

    @Test
    void shouldRoundTripExamPersistenceModel() {
        Exam exam = new Exam(
                new ExamId(1002L), new CourseId(1001L), "Midterm",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), ExamStatus.DRAFT);
        ExamConverter converter = new ExamConverter();

        Exam restored = converter.toDomain(converter.toPo(exam));

        assertEquals(1002L, restored.getId().value());
        assertEquals(1001L, restored.getCourseId().value());
    }
}
