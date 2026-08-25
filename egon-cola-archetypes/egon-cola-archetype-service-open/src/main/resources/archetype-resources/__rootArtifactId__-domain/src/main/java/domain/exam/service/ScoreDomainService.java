#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.service;

import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.common.Page;
import ${package}.domain.exam.vos.ExamId;
import java.util.Optional;
import top.egon.cola.component.common.mybatis.extension.EgonColaIService;
import top.egon.cola.component.common.mybatis.model.EgonModel;

public interface ScoreDomainService<P extends EgonModel<P>> extends EgonColaIService<P> {
    Score recordScore(
            Exam exam,
            ExamPaper paper,
            Long studentId,
            int points,
            boolean duplicate);
    Score save(Score score);
    Optional<Score> findByExamIdAndId(ExamId examId, Long scoreId);
    boolean existsByExamIdAndStudentId(ExamId examId, Long studentId);
    Page<Score> findPageByExamId(ExamId examId, int currentPage, int pageSize);
}
