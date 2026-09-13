package top.egon.cola.archetype.source.serviceopen.domain.exam.service;

import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Score;
import top.egon.cola.archetype.source.serviceopen.domain.common.Page;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ExamId;
import java.util.Optional;

public interface ScoreDomainService {
    Score recordScore( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull Exam exam, @jakarta.validation.Valid @jakarta.validation.constraints.NotNull ExamPaper paper,
            Long studentId,
            int points,
            boolean duplicate);
    Score save( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull Score score);
    Optional<Score> findByExamIdAndId( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull ExamId examId, Long scoreId);
    boolean existsByExamIdAndStudentId( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull ExamId examId, Long studentId);
    Page<Score> findPageByExamId( @jakarta.validation.Valid @jakarta.validation.constraints.NotNull ExamId examId, int currentPage, int pageSize);
}
