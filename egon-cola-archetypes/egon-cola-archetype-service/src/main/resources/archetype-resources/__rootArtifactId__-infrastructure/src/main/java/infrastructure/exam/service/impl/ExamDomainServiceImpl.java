#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.service.impl;

import ${package}.domain.course.entities.Course;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.enums.ExamPaperStatus;
import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.exam.service.ExamDomainService;
import ${package}.domain.exam.validators.ExamDomainValidator;
import ${package}.domain.exam.vos.ExamId;
import ${package}.infrastructure.exam.repo.converter.ExamConverter;
import ${package}.infrastructure.exam.repo.converter.ExamPaperConverter;
import ${package}.infrastructure.exam.repo.dao.ExamDAO;
import ${package}.infrastructure.exam.repo.dao.ExamPaperDAO;
import ${package}.infrastructure.exam.repo.po.ExamPO;
import ${package}.infrastructure.exam.repo.po.ExamPaperPO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service("examDomainService")
@RequiredArgsConstructor
public class ExamDomainServiceImpl
        extends EgonColaServiceImpl<ExamDAO, ExamPO>
        implements ExamDomainService<ExamPO> {

    @Qualifier("examDAO")
    private final ExamDAO examDAO;
    @Qualifier("examPaperDAO")
    private final ExamPaperDAO examPaperDAO;
    @Qualifier("examConverterImpl")
    private final ExamConverter examConverter;
    @Qualifier("examPaperConverterImpl")
    private final ExamPaperConverter examPaperConverter;
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

    private final ExamDomainValidator validator = new ExamDomainValidator();

    @Override
    public Exam createExam(Course course, String title, java.time.Instant startsAt, java.time.Instant endsAt) {
        validator.validateExam(course, title, startsAt, endsAt);
        return new Exam(
                new ExamId(idGenerator.nextLongId()), new CourseId(course.getId()), title.trim(),
                startsAt, endsAt, ExamStatus.DRAFT);
    }

    @Override
    public ExamPaper attachPaper(Exam exam, String title, int totalPoints) {
        validator.validatePaper(title, totalPoints);
        return new ExamPaper(
                idGenerator.nextLongId(), exam.getId(), title.trim(), totalPoints,
                ExamPaperStatus.DRAFT);
    }

    @Override
    public Exam publishExam(Exam exam, ExamPaper paper) {
        if (exam == null || paper == null || !paper.getExamId().equals(exam.getId())) {
            throw new ${package}.domain.common.EvaluationDomainException(
                    ${package}.domain.common.EvaluationDomainErrorCode.EXAM_NOT_PUBLISHABLE,
                    "exam requires its own paper before publication");
        }
        exam.publish();
        paper.publish();
        return exam;
    }

    @Override
    public Exam save(Exam exam) {
        ExamPO po = examConverter.toTarget(exam);
        po.setId(exam.getId().value());
        super.save(po);
        return examConverter.toSource(po);
    }

    @Override
    public Optional<Exam> findById(ExamId examId) {
        return Optional.ofNullable(super.getById(examId.value())).map(examConverter::toSource);
    }

    @Override
    public ExamPaper savePaper(ExamPaper paper) {
        ExamPaperPO po = examPaperConverter.toTarget(paper);
        po.setId(paper.getId());
        insertCompanion(examPaperDAO, po);
        return examPaperConverter.toSource(po);
    }

    @Override
    public Optional<ExamPaper> findPaperByExamId(ExamId examId) {
        return Optional.ofNullable(examPaperDAO.selectByExamId(examId.value()))
                .map(examPaperConverter::toSource);
    }

    private <M extends top.egon.cola.component.common.mybatis.model.EgonModel<M>> void insertCompanion(
            EgonColaMapper<M> dao, M model) {
        if (tenantIdProvider.currentTenantId() == null) {
            throw new IllegalStateException("TENANT_CONTEXT_MISSING");
        }
        modelValidationUtils.validateBusiness(model, EgonColaModelValidationGroups.Operation.INSERT);
        dao.insert(model);
    }
}
