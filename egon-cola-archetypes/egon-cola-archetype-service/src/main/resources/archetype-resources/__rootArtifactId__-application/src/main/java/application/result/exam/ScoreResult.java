#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.result.exam;

public record ScoreResult(
        String id,
        String examId,
        String courseId,
        String studentId,
        int points,
        String status) {
}
