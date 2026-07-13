#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.event;

import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.entities.Score;

public interface ExamEventPublisher {
    void examPublished(Exam exam, ExamPaper paper);
    void scoreRecorded(Score score);
}
