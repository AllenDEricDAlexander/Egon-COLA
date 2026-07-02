#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.dto;

public record ExamResultMessage(String courseId, String studentId, int score) {
}
