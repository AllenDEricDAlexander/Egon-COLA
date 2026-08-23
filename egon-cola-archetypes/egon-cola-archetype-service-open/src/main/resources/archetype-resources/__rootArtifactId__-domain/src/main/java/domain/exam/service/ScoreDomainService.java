#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.service;

import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;
import ${package}.domain.exam.entities.Score;

public interface ScoreDomainService {
    Score recordScore(
            long id,
            Exam exam,
            ExamPaper paper,
            long studentId,
            int points,
            boolean duplicate);
}
