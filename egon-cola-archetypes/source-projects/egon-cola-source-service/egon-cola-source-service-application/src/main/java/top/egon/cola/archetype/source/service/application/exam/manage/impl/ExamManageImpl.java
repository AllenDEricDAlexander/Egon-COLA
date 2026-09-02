package top.egon.cola.archetype.source.service.application.exam.manage.impl;

import top.egon.cola.archetype.source.service.application.exceptions.ApplicationErrorCode;
import top.egon.cola.archetype.source.service.application.exceptions.ApplicationException;
import top.egon.cola.archetype.source.service.application.exam.command.AttachExamPaperCommand;
import top.egon.cola.archetype.source.service.application.exam.command.CreateExamCommand;
import top.egon.cola.archetype.source.service.application.exam.command.PublishExamCommand;
import top.egon.cola.archetype.source.service.application.exam.converter.ExamApplicationConverter;
import top.egon.cola.archetype.source.service.application.exam.manage.ExamManage;
import top.egon.cola.archetype.source.service.application.exam.query.GetExamQuery;
import top.egon.cola.archetype.source.service.application.exam.result.ExamDetailResult;
import top.egon.cola.archetype.source.service.application.exam.result.ExamPaperResult;
import top.egon.cola.archetype.source.service.application.exam.validators.ExamApplicationValidator;
import top.egon.cola.archetype.source.service.domain.course.entities.Course;
import top.egon.cola.archetype.source.service.domain.course.service.CourseDomainService;
import top.egon.cola.archetype.source.service.domain.course.vos.CourseId;
import top.egon.cola.archetype.source.service.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.service.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.service.domain.exam.event.ExamEventPublisher;
import top.egon.cola.archetype.source.service.domain.exam.service.ExamDomainService;
import top.egon.cola.archetype.source.service.domain.exam.vos.ExamId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("evaluationExamManage")
@RequiredArgsConstructor
public class ExamManageImpl implements ExamManage {

    private final CourseDomainService<?> courseDomainService;
    private final ExamDomainService<?> examDomainService;
    private final ExamEventPublisher examEventPublisher;
    private final ExamApplicationConverter converter;
    private final ExamApplicationValidator validator;

    @Override
    @Transactional
    public ExamDetailResult create(CreateExamCommand command) {
        validator.positive(command.courseId(), "courseId");
        Course course = courseDomainService.findById(new CourseId(command.courseId()))
                .orElseThrow(() -> failure(ApplicationErrorCode.COURSE_NOT_FOUND, "course not found"));
        Exam exam = examDomainService.createExam(
                course, command.title(), command.startsAt(), command.endsAt());
        return converter.toResult(examDomainService.save(exam));
    }

    @Override
    @Transactional
    public ExamPaperResult attachPaper(AttachExamPaperCommand command) {
        validator.positive(command.examId(), "examId");
        Exam exam = requireExam(command.examId());
        ExamPaper paper = examDomainService.attachPaper(exam, command.title(), command.totalPoints());
        return converter.toResult(examDomainService.savePaper(paper));
    }

    @Override
    @Transactional
    public ExamDetailResult publish(PublishExamCommand command) {
        validator.positive(command.examId(), "examId");
        Exam exam = requireExam(command.examId());
        ExamPaper paper = examDomainService.findPaperByExamId(exam.getId())
                .orElseThrow(() -> failure(
                        ApplicationErrorCode.EXAM_PAPER_NOT_FOUND, "exam paper not found"));
        examDomainService.publishExam(exam, paper);
        Exam saved = examDomainService.save(exam);
        examDomainService.savePaper(paper);
        examEventPublisher.examPublished(saved, paper);
        return converter.toResult(saved);
    }

    @Override
    public ExamDetailResult get(GetExamQuery query) {
        validator.positive(query.examId(), "examId");
        return converter.toResult(requireExam(query.examId()));
    }

    private Exam requireExam(Long examId) {
        return examDomainService.findById(new ExamId(examId))
                .orElseThrow(() -> failure(ApplicationErrorCode.EXAM_NOT_FOUND, "exam not found"));
    }

    private static ApplicationException failure(ApplicationErrorCode code, String message) {
        return new ApplicationException(code, message);
    }
}
