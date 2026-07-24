#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.enums.ExamPaperStatus;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.converter.ExamPaperConverter;
import ${package}.infrastructure.exam.repo.jpa.ExamPaperJpaRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class ExamPaperRepositoryTest {
    @Test void shouldRoundTripPaper() {
        var converter = new ExamPaperConverter();
        var paper = new ExamPaper("paper-1", new ExamId("exam-1"), "Paper", 100, ExamPaperStatus.DRAFT);
        assertEquals(100, converter.toDomain(converter.toPo(paper, Instant.EPOCH)).getTotalPoints());
    }

    @Test
    void shouldExposeExamAwarePointLookup() throws NoSuchMethodException {
        ExamPaperJpaRepository.class.getMethod(
                "findByExamIdAndId", String.class, String.class);
    }
}
