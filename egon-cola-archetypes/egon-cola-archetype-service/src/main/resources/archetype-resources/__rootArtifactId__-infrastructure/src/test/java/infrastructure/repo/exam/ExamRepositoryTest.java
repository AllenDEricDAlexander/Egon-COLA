#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.exam;

import ${package}.domain.entities.exam.Exam;
import ${package}.domain.enums.exam.ExamStatus;
import ${package}.domain.vos.course.CourseId;
import ${package}.domain.vos.exam.ExamId;
import ${package}.infrastructure.repo.exam.converter.ExamConverter;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamRepositoryTest {

    @Test
    void shouldRoundTripExamPersistenceModel() {
        Exam exam = new Exam(
                new ExamId("exam-1"), new CourseId("course-1"), "Midterm",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(60), ExamStatus.DRAFT);
        ExamConverter converter = new ExamConverter();

        Exam restored = converter.toDomain(converter.toPo(exam));

        assertEquals("exam-1", restored.getId().value());
        assertEquals("course-1", restored.getCourseId().value());
    }
}
