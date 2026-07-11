#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.repos.exam;

import ${package}.domain.entities.exam.ExamPaper;
import ${package}.domain.vos.exam.ExamId;
import java.util.Optional;

public interface ExamPaperRepository {
    ExamPaper save(ExamPaper paper);
    Optional<ExamPaper> findByExamId(ExamId examId);
}
