package top.egon.cola.archetype.source.serviceopen.infrastructure.exam.service.impl;

import top.egon.cola.archetype.source.serviceopen.domain.common.Page;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Score;
import top.egon.cola.archetype.source.serviceopen.domain.exam.enums.ScoreStatus;
import top.egon.cola.archetype.source.serviceopen.domain.exam.service.ScoreDomainService;
import top.egon.cola.archetype.source.serviceopen.domain.exam.validators.ScoreDomainValidator;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ExamId;
import top.egon.cola.archetype.source.serviceopen.domain.exam.vos.ScoreValue;
import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.converter.ScoreConverter;
import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.repo.ScoreRepository;
import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.po.ScorePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;

import java.util.Optional;

@Slf4j
@Validated
@Service("scoreDomainService")
@RequiredArgsConstructor
public class ScoreDomainServiceImpl
        implements ScoreDomainService {

    @Qualifier("scoreRepository")
    private final ScoreRepository scoreRepository;
    @Qualifier("scoreConverterImpl")
    private final ScoreConverter scoreConverter;

    @Qualifier("scoreDomainValidator")
    private final ScoreDomainValidator validator;

    @Override
    public Score recordScore(
            Exam exam, ExamPaper paper, Long studentId, int points, boolean duplicate) {
        validator.validate(exam, paper, studentId, points, duplicate);
        return new Score(
                SnowflakeIdGenerator.nextLongId(), exam.getId(), exam.getCourseId(), studentId,
                new ScoreValue(points), ScoreStatus.RECORDED);
    }

    @Override
    @Transactional
    public Score save(Score score) {
        ScorePO po = scoreConverter.toTarget(score);
        ScorePO current = po.getId() == null ? null : scoreRepository.getById(po.getId());
        if (current != null) { scoreConverter.updateMetadata(po, current); }
        boolean written = current == null ? scoreRepository.save(po) : scoreRepository.updateById(po);
        if (!written) { throw new org.springframework.dao.OptimisticLockingFailureException("VERSIONED_WRITE_CONFLICT"); }

        return scoreConverter.toSource(po);
    }

    @Override
    public Optional<Score> findByExamIdAndId(ExamId examId, Long scoreId) {
        return Optional.ofNullable(scoreRepository.selectByExamIdAndId(examId.value(), scoreId))
                .map(scoreConverter::toSource);
    }

    @Override
    public boolean existsByExamIdAndStudentId(ExamId examId, Long studentId) {
        return scoreRepository.countByExamIdAndStudentId(examId.value(), studentId) > 0;
    }

    @Override
    public Page<Score> findPageByExamId(ExamId examId, int currentPage, int pageSize) {
        int normalizedPage = Math.max(currentPage, 1);
        int offset = (normalizedPage - 1) * pageSize;
        long total = scoreRepository.countByExamId(examId.value());
        return Page.of(
                scoreRepository.selectPageByExamId(examId.value(), pageSize, offset).stream()
                        .map(scoreConverter::toSource).toList(),
                currentPage,
                (int) Math.ceil((double) total / pageSize),
                pageSize,
                total);
    }
}
