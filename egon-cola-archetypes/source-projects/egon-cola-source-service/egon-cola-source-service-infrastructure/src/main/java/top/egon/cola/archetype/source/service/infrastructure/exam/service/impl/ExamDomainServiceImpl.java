package top.egon.cola.archetype.source.service.infrastructure.exam.service.impl;

import top.egon.cola.archetype.source.service.domain.course.entities.Course;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.service.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.service.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.service.domain.exam.enums.ExamPaperStatus;
import top.egon.cola.archetype.source.service.domain.exam.enums.ExamStatus;
import top.egon.cola.archetype.source.service.domain.exam.service.ExamDomainService;
import top.egon.cola.archetype.source.service.domain.exam.validators.ExamDomainValidator;
import top.egon.cola.archetype.source.service.domain.exam.vos.ExamId;
import top.egon.cola.archetype.source.service.infrastructure.exam.converter.ExamConverter;
import top.egon.cola.archetype.source.service.infrastructure.exam.converter.ExamPaperConverter;
import top.egon.cola.archetype.source.service.infrastructure.exam.repo.ExamRepository;
import top.egon.cola.archetype.source.service.infrastructure.exam.repo.ExamPaperRepository;
import top.egon.cola.archetype.source.service.infrastructure.exam.po.ExamPO;
import top.egon.cola.archetype.source.service.infrastructure.exam.po.ExamPaperPO;
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
@Service("examDomainService")
@RequiredArgsConstructor
public class ExamDomainServiceImpl
        implements ExamDomainService {

    @Qualifier("examRepository")
    private final ExamRepository examRepository;
    @Qualifier("examPaperRepository")
    private final ExamPaperRepository examPaperRepository;
    @Qualifier("examConverterImpl")
    private final ExamConverter examConverter;
    @Qualifier("examPaperConverterImpl")
    private final ExamPaperConverter examPaperConverter;

    @Qualifier("examDomainValidator")
    private final ExamDomainValidator validator;

    @Override
    public Exam createExam(Course course, String title, java.time.Instant startsAt, java.time.Instant endsAt) {
        validator.validateExam(course, title, startsAt, endsAt);
        return new Exam(
                new ExamId(SnowflakeIdGenerator.nextLongId()), new CourseId(course.getId()), title.trim(),
                startsAt, endsAt, ExamStatus.DRAFT);
    }

    @Override
    public ExamPaper attachPaper(Exam exam, String title, int totalPoints) {
        validator.validatePaper(title, totalPoints);
        return new ExamPaper(
                SnowflakeIdGenerator.nextLongId(), exam.getId(), title.trim(), totalPoints,
                ExamPaperStatus.DRAFT);
    }

    @Override
    public Exam publishExam(Exam exam, ExamPaper paper) {
        if (exam == null || paper == null || !paper.getExamId().equals(exam.getId())) {
            throw new top.egon.cola.archetype.source.service.common.exception.EvaluationDomainException(
                    top.egon.cola.archetype.source.service.common.enums.EvaluationDomainErrorCode.EXAM_NOT_PUBLISHABLE,
                    "exam requires its own paper before publication");
        }
        exam.publish();
        paper.publish();
        return exam;
    }

    @Override
    @Transactional
    public Exam save(Exam exam) {
        ExamPO po = examConverter.toTarget(exam);
        ExamPO current = po.getId() == null ? null : examRepository.getById(po.getId());
        if (current != null) { examConverter.updateMetadata(po, current); }
        boolean written = current == null ? examRepository.save(po) : examRepository.updateById(po);
        if (!written) { throw new org.springframework.dao.OptimisticLockingFailureException("VERSIONED_WRITE_CONFLICT"); }

        return examConverter.toSource(po);
    }

    @Override
    public Optional<Exam> findById(ExamId examId) {
        return Optional.ofNullable(examRepository.getById(examId.value())).map(examConverter::toSource);
    }

    @Override
    @Transactional
    public ExamPaper savePaper(ExamPaper paper) {
        ExamPaperPO po = examPaperConverter.toTarget(paper);
        ExamPaperPO current = po.getId() == null ? null : examPaperRepository.getById(po.getId());
        if (current != null) { examPaperConverter.updateMetadata(po, current); }
        boolean written = current == null ? examPaperRepository.save(po) : examPaperRepository.updateById(po);
        if (!written) { throw new org.springframework.dao.OptimisticLockingFailureException("VERSIONED_WRITE_CONFLICT"); }

        return examPaperConverter.toSource(po);
    }

    @Override
    public Optional<ExamPaper> findPaperByExamId(ExamId examId) {
        return Optional.ofNullable(examPaperRepository.selectByExamId(examId.value()))
                .map(examPaperConverter::toSource);
    }

}
