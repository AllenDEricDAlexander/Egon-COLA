#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.result;

public record ExamPaperResult(
        String id, String examId, String title, int totalPoints, String status) {
}
