#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.manage.exam.impl;

import ${package}.application.command.exam.AttachExamPaperCommand;
import ${package}.application.command.exam.CreateExamCommand;
import ${package}.application.command.exam.PublishExamCommand;
import ${package}.application.converter.exam.ExamApplicationConverter;
import ${package}.application.exceptions.ApplicationErrorCode;
import ${package}.application.exceptions.ApplicationException;
import ${package}.application.manage.exam.ExamManage;
import ${package}.application.query.exam.GetExamQuery;
import ${package}.application.result.exam.ExamDetailResult;
import ${package}.application.result.exam.ExamPaperResult;
import ${package}.application.validators.exam.ExamApplicationValidator;
import ${package}.domain.course.entities.Course;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.event.ExamEventPublisher;
import ${package}.domain.course.repos.CourseRepository;
import ${package}.domain.exam.repos.ExamPaperRepository;
import ${package}.domain.exam.repos.ExamRepository;
import ${package}.domain.exam.service.ExamDomainService;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.vos.ExamId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("evaluationExamManage")
@RequiredArgsConstructor
public class ExamManageImpl implements ExamManage {

    private final CourseRepository courseRepository;
    private final ExamRepository examRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamEventPublisher examEventPublisher;
    private final ExamDomainService examDomainService;
    private final ExamApplicationConverter converter;
    private final ExamApplicationValidator validator;

    @Override
    @Transactional
    public ExamDetailResult create(CreateExamCommand command) {
        validator.notBlank(command.courseId(), "courseId");
        validator.notBlank(command.title(), "title");
        Course course = courseRepository.findById(new CourseId(command.courseId()))
                .orElseThrow(() -> failure(ApplicationErrorCode.COURSE_NOT_FOUND, "course not found"));
        Exam exam = examDomainService.createExam(
                UUID.randomUUID().toString(), course, command.title(),
                command.startsAt(), command.endsAt());
        return converter.toResult(examRepository.save(exam));
    }

    @Override
    @Transactional
    public ExamPaperResult attachPaper(AttachExamPaperCommand command) {
        Exam exam = requireExam(command.examId());
        ExamPaper paper = examDomainService.attachPaper(
                UUID.randomUUID().toString(), exam, command.title(), command.totalPoints());
        return converter.toResult(examPaperRepository.save(paper));
    }

    @Override
    @Transactional
    public ExamDetailResult publish(PublishExamCommand command) {
        Exam exam = requireExam(command.examId());
        ExamPaper paper = examPaperRepository.findByExamId(exam.getId())
                .orElseThrow(() -> failure(
                        ApplicationErrorCode.EXAM_PAPER_NOT_FOUND, "exam paper not found"));
        examDomainService.publishExam(exam, paper);
        Exam saved = examRepository.save(exam);
        examPaperRepository.save(paper);
        examEventPublisher.examPublished(saved, paper);
        return converter.toResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamDetailResult get(GetExamQuery query) {
        return converter.toResult(requireExam(query.examId()));
    }

    private Exam requireExam(String examId) {
        validator.notBlank(examId, "examId");
        return examRepository.findById(new ExamId(examId))
                .orElseThrow(() -> failure(ApplicationErrorCode.EXAM_NOT_FOUND, "exam not found"));
    }

    private static ApplicationException failure(ApplicationErrorCode code, String message) {
        return new ApplicationException(code, message);
    }
}
