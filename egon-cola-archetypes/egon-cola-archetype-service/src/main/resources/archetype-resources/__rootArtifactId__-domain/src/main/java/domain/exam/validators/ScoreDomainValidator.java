#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.validators;

import ${package}.domain.common.EvaluationDomainErrorCode;
import ${package}.domain.common.EvaluationDomainException;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.enums.ExamStatus;

public final class ScoreDomainValidator {

    public void validate(
            Exam exam, ExamPaper paper, String studentId, int points, boolean duplicate) {
        if (exam == null || paper == null || exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.EXAM_NOT_PUBLISHABLE, "score requires a published exam");
        }
        if (studentId == null || studentId.isBlank() || points < 0
                || points > paper.getTotalPoints()) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.SCORE_OUT_OF_RANGE, "score is outside paper range");
        }
        if (duplicate) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.SCORE_DUPLICATED, "student score already exists");
        }
    }
}
