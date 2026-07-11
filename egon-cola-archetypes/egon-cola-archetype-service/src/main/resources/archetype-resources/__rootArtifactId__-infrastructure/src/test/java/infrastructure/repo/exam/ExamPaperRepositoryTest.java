#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.exam;
import ${package}.domain.entities.exam.ExamPaper;
import ${package}.domain.enums.exam.ExamPaperStatus;
import ${package}.domain.vos.exam.ExamId;
import ${package}.infrastructure.repo.exam.converter.ExamPaperConverter;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class ExamPaperRepositoryTest {
    @Test void shouldRoundTripPaper() {
        var converter = new ExamPaperConverter();
        var paper = new ExamPaper("paper-1", new ExamId("exam-1"), "Paper", 100, ExamPaperStatus.DRAFT);
        assertEquals(100, converter.toDomain(converter.toPo(paper, Instant.EPOCH)).getTotalPoints());
    }
}
