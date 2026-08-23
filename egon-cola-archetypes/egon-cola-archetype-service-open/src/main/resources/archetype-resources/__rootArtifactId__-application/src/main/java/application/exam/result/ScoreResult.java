#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.result;

public record ScoreResult(
        long id,
        long examId,
        long courseId,
        long studentId,
        int points,
        String status) {
}
