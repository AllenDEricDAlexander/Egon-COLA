#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.repos.exam;

import ${package}.domain.common.Page;
import ${package}.domain.entities.exam.Score;
import ${package}.domain.vos.exam.ExamId;
import java.util.Optional;

public interface ScoreRepository {
    Score save(Score score);
    Optional<Score> findById(String scoreId);
    boolean existsByExamIdAndStudentId(ExamId examId, String studentId);
    Page<Score> findPageByExamId(ExamId examId, int currentPage, int pageSize);
}
