#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.manage.impl;

import ${package}.application.exceptions.ApplicationErrorCode;
import ${package}.application.exceptions.ApplicationException;
import ${package}.application.exam.command.AttachExamPaperCommand;
import ${package}.application.exam.command.CreateExamCommand;
import ${package}.application.exam.command.PublishExamCommand;
import ${package}.application.exam.converter.ExamApplicationConverter;
import ${package}.application.exam.manage.ExamManage;
import ${package}.application.exam.query.GetExamQuery;
import ${package}.application.exam.result.ExamDetailResult;
import ${package}.application.exam.result.ExamPaperResult;
import ${package}.application.exam.validators.ExamApplicationValidator;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.service.CourseDomainService;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.event.ExamEventPublisher;
import ${package}.domain.exam.service.ExamDomainService;
import ${package}.domain.exam.vos.ExamId;
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
