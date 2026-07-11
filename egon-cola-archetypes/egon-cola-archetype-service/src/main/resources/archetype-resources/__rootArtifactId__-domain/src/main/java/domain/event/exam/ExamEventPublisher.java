#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.event.exam;

import ${package}.domain.entities.exam.Exam;
import ${package}.domain.entities.exam.ExamPaper;
import ${package}.domain.entities.exam.Score;

public interface ExamEventPublisher {
    void examPublished(Exam exam, ExamPaper paper);
    void scoreRecorded(Score score);
}
