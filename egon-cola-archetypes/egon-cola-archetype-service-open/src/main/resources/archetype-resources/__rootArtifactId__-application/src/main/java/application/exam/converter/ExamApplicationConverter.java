#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.converter;

import ${package}.application.exam.result.ExamDetailResult;
import ${package}.application.exam.result.ExamPaperResult;
import ${package}.application.exam.result.ScoreResult;
import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.entities.Score;
import org.springframework.stereotype.Component;

@Component
public class ExamApplicationConverter {
    public ExamDetailResult toResult(Exam exam) {
        return new ExamDetailResult(
                exam.getId().value(), exam.getCourseId().value(), exam.getTitle(),
                exam.getStartsAt(), exam.getEndsAt(), exam.getStatus().name());
    }

    public ExamPaperResult toResult(ExamPaper paper) {
        return new ExamPaperResult(
                paper.getId(), paper.getExamId().value(), paper.getTitle(),
                paper.getTotalPoints(), paper.getStatus().name());
    }

    public ScoreResult toResult(Score score) {
        return new ScoreResult(
                score.getId(), score.getExamId().value(), score.getCourseId().value(),
                score.getStudentId(), score.getPoints().value(), score.getStatus().name());
    }
}
