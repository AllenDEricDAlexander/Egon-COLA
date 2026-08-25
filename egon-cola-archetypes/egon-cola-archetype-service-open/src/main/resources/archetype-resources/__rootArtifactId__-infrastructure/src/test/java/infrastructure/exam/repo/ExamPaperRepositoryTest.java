#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo;

import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.enums.ExamPaperStatus;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.converter.ExamPaperConverter;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamPaperRepositoryTest {
    @Test
    void shouldRoundTripPaper() {
        ExamPaper paper = new ExamPaper(5001L, new ExamId(4001L), "Paper", 100,
                ExamPaperStatus.DRAFT);
        ExamPaperConverter converter = Mappers.getMapper(ExamPaperConverter.class);
        var target = converter.toTarget(paper);
        target.setId(paper.getId());
        ExamPaper restored = converter.toSource(target);
        assertEquals(5001L, restored.getId());
        assertEquals(4001L, restored.getExamId().value());
    }
}
