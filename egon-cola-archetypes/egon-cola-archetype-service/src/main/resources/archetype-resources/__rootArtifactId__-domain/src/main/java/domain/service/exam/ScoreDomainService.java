#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.service.exam;

import ${package}.domain.entities.exam.Exam;
import ${package}.domain.entities.exam.ExamPaper;
import ${package}.domain.entities.exam.Score;

public interface ScoreDomainService {
    Score recordScore(
            String id,
            Exam exam,
            ExamPaper paper,
            String studentId,
            int points,
            boolean duplicate);
}
