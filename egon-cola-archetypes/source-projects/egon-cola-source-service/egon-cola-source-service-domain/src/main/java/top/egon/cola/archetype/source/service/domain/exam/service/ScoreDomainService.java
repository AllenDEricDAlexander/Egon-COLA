package top.egon.cola.archetype.source.service.domain.exam.service;

import top.egon.cola.archetype.source.service.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.service.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.service.domain.exam.entities.Score;
import top.egon.cola.archetype.source.service.domain.common.Page;
import top.egon.cola.archetype.source.service.domain.exam.vos.ExamId;
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
