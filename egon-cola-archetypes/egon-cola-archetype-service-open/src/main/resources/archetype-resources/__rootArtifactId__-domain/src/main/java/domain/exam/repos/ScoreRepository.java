#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.repos;

import ${package}.domain.common.Page;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.vos.ExamId;
import java.util.Optional;

public interface ScoreRepository {
    Score save(Score score);
    Optional<Score> findByExamIdAndId(ExamId examId, String scoreId);
    boolean existsByExamIdAndStudentId(ExamId examId, String studentId);
    Page<Score> findPageByExamId(ExamId examId, int currentPage, int pageSize);
}
