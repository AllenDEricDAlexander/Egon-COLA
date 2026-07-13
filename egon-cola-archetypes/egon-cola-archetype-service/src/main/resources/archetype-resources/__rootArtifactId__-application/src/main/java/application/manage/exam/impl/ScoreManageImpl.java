#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.exam.impl;

import ${package}.application.command.exam.RecordScoreCommand;
import ${package}.application.converter.exam.ExamApplicationConverter;
import ${package}.application.exceptions.ApplicationErrorCode;
import ${package}.application.exceptions.ApplicationException;
import ${package}.application.manage.exam.ScoreManage;
import ${package}.application.query.exam.GetScoreQuery;
import ${package}.application.query.exam.PageScoreQuery;
import ${package}.application.result.exam.ScoreResult;
import ${package}.application.result.PageResult;
import ${package}.application.validators.exam.ExamApplicationValidator;
import ${package}.domain.common.Page;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.event.ExamEventPublisher;
import ${package}.domain.exam.repos.ExamPaperRepository;
import ${package}.domain.exam.repos.ExamRepository;
import ${package}.domain.exam.repos.ScoreRepository;
import ${package}.domain.exam.service.ScoreDomainService;
import ${package}.domain.exam.vos.ExamId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("scoreManage")
@RequiredArgsConstructor
public class ScoreManageImpl implements ScoreManage {

    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ScoreRepository scoreRepository;
    private final ExamEventPublisher examEventPublisher;
    private final ScoreDomainService scoreDomainService;
    private final ExamApplicationConverter converter;
    private final ExamApplicationValidator validator;

    @Override
    @Transactional
    public ScoreResult record(RecordScoreCommand command) {
        validator.notBlank(command.examId(), "examId");
        validator.notBlank(command.studentId(), "studentId");
        ExamId examId = new ExamId(command.examId());
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> failure(ApplicationErrorCode.EXAM_NOT_FOUND, "exam not found"));
        ExamPaper paper = examPaperRepository.findByExamId(examId)
                .orElseThrow(() -> failure(
                        ApplicationErrorCode.EXAM_PAPER_NOT_FOUND, "exam paper not found"));
        boolean duplicate = scoreRepository.existsByExamIdAndStudentId(
                examId, command.studentId());
        Score score = scoreDomainService.recordScore(
                UUID.randomUUID().toString(), exam, paper,
                command.studentId(), command.points(), duplicate);
        Score saved = scoreRepository.save(score);
        examEventPublisher.scoreRecorded(saved);
        return converter.toResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ScoreResult get(GetScoreQuery query) {
        validator.notBlank(query.scoreId(), "scoreId");
        return scoreRepository.findById(query.scoreId())
                .map(converter::toResult)
                .orElseThrow(() -> failure(ApplicationErrorCode.SCORE_NOT_FOUND, "score not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ScoreResult> page(PageScoreQuery query) {
        validator.notBlank(query.examId(), "examId");
        Page<Score> page = scoreRepository.findPageByExamId(
                new ExamId(query.examId()), query.currentPage(), query.pageSize());
        return PageResult.of(
                page.records().stream().map(converter::toResult).toList(),
                page.currentPage(), page.totalPages(), page.pageSize(), page.totalCount());
    }

    private static ApplicationException failure(ApplicationErrorCode code, String message) {
        return new ApplicationException(code, message);
    }
}
