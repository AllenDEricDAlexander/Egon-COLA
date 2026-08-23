#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.service.impl;

import ${package}.domain.common.EvaluationDomainErrorCode;
import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.course.entities.Course;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.enums.ExamPaperStatus;
import ${package}.domain.exam.enums.ExamStatus;
import ${package}.domain.exam.service.ExamDomainService;
import ${package}.domain.exam.validators.ExamDomainValidator;
import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.vos.ExamId;
import java.time.Instant;

public final class ExamDomainServiceImpl implements ExamDomainService {

    private final ExamDomainValidator validator = new ExamDomainValidator();

    @Override
    public Exam createExam(
            String id, Course course, String title, Instant startsAt, Instant endsAt) {
        validator.validateExam(course, title, startsAt, endsAt);
        return new Exam(
                new ExamId(id), new CourseId(course.getId()), title.trim(),
                startsAt, endsAt, ExamStatus.DRAFT);
    }

    @Override
    public ExamPaper attachPaper(String id, Exam exam, String title, int totalPoints) {
        validator.validatePaper(title, totalPoints);
        return new ExamPaper(
                id, exam.getId(), title.trim(), totalPoints, ExamPaperStatus.DRAFT);
    }

    @Override
    public Exam publishExam(Exam exam, ExamPaper paper) {
        if (exam == null || paper == null || !paper.getExamId().equals(exam.getId())) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.EXAM_NOT_PUBLISHABLE,
                    "exam requires its own paper before publication");
        }
        exam.publish();
        paper.publish();
        return exam;
    }
}
