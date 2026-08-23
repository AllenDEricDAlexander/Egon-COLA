#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.repos;

import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.vos.ExamId;
import java.util.Optional;

public interface ExamPaperRepository {
    ExamPaper save(ExamPaper paper);
    Optional<ExamPaper> findByExamId(ExamId examId);
}
