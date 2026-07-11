#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.aggregates.exam;

import ${package}.domain.entities.exam.Exam;
import ${package}.domain.entities.exam.ExamPaper;

public record ExamAggregate(Exam exam, ExamPaper paper) {
}
