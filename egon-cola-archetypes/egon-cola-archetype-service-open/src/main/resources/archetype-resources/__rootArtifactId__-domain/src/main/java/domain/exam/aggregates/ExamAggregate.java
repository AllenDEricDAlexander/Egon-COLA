#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.aggregates;

import ${package}.domain.exam.entities.Exam;
import ${package}.domain.exam.entities.ExamPaper;

public record ExamAggregate(Exam exam, ExamPaper paper) {
}
