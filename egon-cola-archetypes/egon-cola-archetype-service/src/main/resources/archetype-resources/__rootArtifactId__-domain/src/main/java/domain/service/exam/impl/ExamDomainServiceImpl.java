#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.service.exam.impl;

import ${package}.domain.common.EvaluationDomainErrorCode;
import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.entities.course.Course;
import ${package}.domain.entities.exam.Exam;
import ${package}.domain.entities.exam.ExamPaper;
import ${package}.domain.enums.exam.ExamPaperStatus;
import ${package}.domain.enums.exam.ExamStatus;
import ${package}.domain.service.exam.ExamDomainService;
import ${package}.domain.validators.exam.ExamDomainValidator;
import ${package}.domain.vos.course.CourseId;
import ${package}.domain.vos.exam.ExamId;
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
