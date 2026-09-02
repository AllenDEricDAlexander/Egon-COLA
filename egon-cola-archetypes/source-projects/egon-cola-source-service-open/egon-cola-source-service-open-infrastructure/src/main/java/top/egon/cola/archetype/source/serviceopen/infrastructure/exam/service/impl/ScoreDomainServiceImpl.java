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
import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.repo.converter.ScoreConverter;
import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.repo.dao.ScoreDAO;
import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.repo.po.ScorePO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

import java.util.Optional;

@Slf4j
@Service("scoreDomainService")
@RequiredArgsConstructor
public class ScoreDomainServiceImpl
        extends EgonColaServiceImpl<ScoreDAO, ScorePO>
        implements ScoreDomainService<ScorePO> {

    @Qualifier("scoreDAO")
    private final ScoreDAO scoreDAO;
    @Qualifier("scoreConverterImpl")
    private final ScoreConverter scoreConverter;
    @Qualifier("snowflakeIdGenerator")
    private final LongIdGenerator idGenerator;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaModelValidationUtils")
    private final EgonColaModelValidationUtils modelValidationUtils;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaMdcTenantIdProvider")
    private final EgonColaTenantIdProvider tenantIdProvider;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    private final ScoreDomainValidator validator = new ScoreDomainValidator();

    @Override
    public Score recordScore(
            Exam exam, ExamPaper paper, Long studentId, int points, boolean duplicate) {
        validator.validate(exam, paper, studentId, points, duplicate);
        return new Score(
                idGenerator.nextLongId(), exam.getId(), exam.getCourseId(), studentId,
                new ScoreValue(points), ScoreStatus.RECORDED);
    }

    @Override
    public Score save(Score score) {
        ScorePO po = scoreConverter.toTarget(score);
        po.setId(score.getId());
        super.save(po);
        return scoreConverter.toSource(po);
    }

    @Override
    public Optional<Score> findByExamIdAndId(ExamId examId, Long scoreId) {
        return Optional.ofNullable(scoreDAO.selectByExamIdAndId(examId.value(), scoreId))
                .map(scoreConverter::toSource);
    }

    @Override
    public boolean existsByExamIdAndStudentId(ExamId examId, Long studentId) {
        return scoreDAO.countByExamIdAndStudentId(examId.value(), studentId) > 0;
    }

    @Override
    public Page<Score> findPageByExamId(ExamId examId, int currentPage, int pageSize) {
        int normalizedPage = Math.max(currentPage, 1);
        int offset = (normalizedPage - 1) * pageSize;
        return Page.of(
                scoreDAO.selectPageByExamId(examId.value(), pageSize, offset).stream()
                        .map(scoreConverter::toSource).toList(),
                currentPage,
                (int) Math.ceil((double) scoreDAO.countByExamId(examId.value()) / pageSize),
                pageSize,
                scoreDAO.countByExamId(examId.value()));
    }
}
