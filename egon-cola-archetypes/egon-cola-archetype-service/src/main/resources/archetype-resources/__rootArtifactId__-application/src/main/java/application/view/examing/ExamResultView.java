#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.view.examing;

public record ExamResultView(String id, String courseId, String studentId, int score, String status) {
}
