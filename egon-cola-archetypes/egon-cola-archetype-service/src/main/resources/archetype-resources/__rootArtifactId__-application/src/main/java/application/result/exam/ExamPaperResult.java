#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.result.exam;

public record ExamPaperResult(
        String id, String examId, String title, int totalPoints, String status) {
}
