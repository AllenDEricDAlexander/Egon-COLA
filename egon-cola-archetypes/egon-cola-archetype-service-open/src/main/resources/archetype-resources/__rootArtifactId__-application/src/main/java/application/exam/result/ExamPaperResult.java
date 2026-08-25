#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.result;

public record ExamPaperResult(
        Long id, Long examId, String title, int totalPoints, String status) {
}
