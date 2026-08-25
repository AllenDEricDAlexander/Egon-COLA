#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.result;

public record ScoreResult(
        Long id,
        Long examId,
        Long courseId,
        Long studentId,
        int points,
        String status) {
}
