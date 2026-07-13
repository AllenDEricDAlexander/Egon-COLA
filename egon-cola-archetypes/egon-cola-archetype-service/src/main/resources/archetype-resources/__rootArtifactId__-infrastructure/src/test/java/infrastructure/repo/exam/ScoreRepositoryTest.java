#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.exam;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.enums.ScoreStatus;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.vos.ExamId;
import ${package}.domain.exam.vos.ScoreValue;
import ${package}.infrastructure.repo.exam.converter.ScoreConverter;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class ScoreRepositoryTest {
    @Test void shouldRoundTripScore() {
        var converter = new ScoreConverter();
        var score = new Score("score-1", new ExamId("exam-1"), new CourseId("course-1"),
                "student-1", new ScoreValue(90), ScoreStatus.RECORDED);
        assertEquals(90, converter.toDomain(converter.toPo(score, Instant.EPOCH)).getPoints().value());
    }
}
