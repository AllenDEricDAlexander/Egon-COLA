package top.egon.cola.archetype.source.serviceopen.application.exam.manage.impl;

import top.egon.cola.archetype.source.serviceopen.application.exceptions.ApplicationErrorCode;
import top.egon.cola.archetype.source.serviceopen.application.exceptions.ApplicationException;
import top.egon.cola.archetype.source.serviceopen.application.exam.command.RecordScoreCommand;
import top.egon.cola.archetype.source.serviceopen.application.exam.converter.ExamApplicationConverter;
import top.egon.cola.archetype.source.serviceopen.application.exam.manage.ScoreManage;
import top.egon.cola.archetype.source.serviceopen.application.exam.query.GetScoreQuery;
import top.egon.cola.archetype.source.serviceopen.application.exam.query.PageScoreQuery;
import top.egon.cola.archetype.source.serviceopen.application.exam.result.ScoreResult;
import top.egon.cola.archetype.source.serviceopen.application.exam.validators.ExamApplicationValidator;
import top.egon.cola.archetype.source.serviceopen.application.result.PageResult;
import top.egon.cola.archetype.source.serviceopen.domain.common.Page;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Score;
import top.egon.cola.archetype.source.serviceopen.domain.exam.event.ExamEventPublisher;
import top.egon.cola.archetype.source.serviceopen.domain.exam.service.ExamDomainService;
import top.egon.cola.archetype.source.serviceopen.domain.exam.service.ScoreDomainService;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ExamId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("scoreManage")
@RequiredArgsConstructor
public class ScoreManageImpl implements ScoreManage {

    private final ExamDomainService<?> examDomainService;
    private final ScoreDomainService<?> scoreDomainService;
    private final ExamEventPublisher examEventPublisher;
    private final ExamApplicationConverter converter;
    private final ExamApplicationValidator validator;

    @Override
    @Transactional
    public ScoreResult record(RecordScoreCommand command) {
        validator.positive(command.examId(), "examId");
        validator.positive(command.studentId(), "studentId");
        ExamId examId = new ExamId(command.examId());
        Exam exam = examDomainService.findById(examId)
                .orElseThrow(() -> failure(ApplicationErrorCode.EXAM_NOT_FOUND, "exam not found"));
        ExamPaper paper = examDomainService.findPaperByExamId(examId)
                .orElseThrow(() -> failure(
                        ApplicationErrorCode.EXAM_PAPER_NOT_FOUND, "exam paper not found"));
        boolean duplicate = scoreDomainService.existsByExamIdAndStudentId(
                examId, command.studentId());
        Score score = scoreDomainService.recordScore(
                exam, paper, command.studentId(), command.points(), duplicate);
        Score saved = scoreDomainService.save(score);
        examEventPublisher.scoreRecorded(saved);
        return converter.toResult(saved);
    }

    @Override
    public ScoreResult get(GetScoreQuery query) {
        validator.positive(query.examId(), "examId");
        validator.positive(query.scoreId(), "scoreId");
        return scoreDomainService.findByExamIdAndId(
                        new ExamId(query.examId()), query.scoreId())
                .map(converter::toResult)
                .orElseThrow(() -> failure(ApplicationErrorCode.SCORE_NOT_FOUND, "score not found"));
    }

    @Override
    public PageResult<ScoreResult> page(PageScoreQuery query) {
        validator.positive(query.examId(), "examId");
        Page<Score> page = scoreDomainService.findPageByExamId(
                new ExamId(query.examId()), query.currentPage(), query.pageSize());
        return PageResult.of(
                page.records().stream().map(converter::toResult).toList(),
                page.currentPage(), page.totalPages(), page.pageSize(), page.totalCount());
    }

    private static ApplicationException failure(ApplicationErrorCode code, String message) {
        return new ApplicationException(code, message);
    }
}
