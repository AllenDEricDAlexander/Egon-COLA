#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.result;

public record ExamPaperResult(
        long id, long examId, String title, int totalPoints, String status) {
}
