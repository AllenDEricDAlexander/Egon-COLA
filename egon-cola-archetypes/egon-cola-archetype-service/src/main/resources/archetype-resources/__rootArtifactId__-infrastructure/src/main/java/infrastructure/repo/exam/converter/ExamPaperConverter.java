#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.exam.converter;
import ${package}.domain.entities.exam.ExamPaper;
import ${package}.domain.enums.exam.ExamPaperStatus;
import ${package}.domain.vos.exam.ExamId;
import ${package}.infrastructure.repo.exam.po.ExamPaperPo;
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
