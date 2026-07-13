#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.converter;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.enums.ExamPaperStatus;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.po.ExamPaperPo;
import java.time.Instant;
import org.springframework.stereotype.Component;
@Component
public class ExamPaperConverter {
    public ExamPaperPo toPo(ExamPaper paper, Instant createdAt) {
        return new ExamPaperPo(paper.getId(), paper.getExamId().value(), paper.getTitle(),
                paper.getTotalPoints(), paper.getStatus().name(), createdAt, Instant.now());
    }
    public ExamPaper toDomain(ExamPaperPo po) {
        return new ExamPaper(po.getId(), new ExamId(po.getExamId()), po.getTitle(),
                po.getTotalPoints(), ExamPaperStatus.valueOf(po.getStatus()));
    }
}
