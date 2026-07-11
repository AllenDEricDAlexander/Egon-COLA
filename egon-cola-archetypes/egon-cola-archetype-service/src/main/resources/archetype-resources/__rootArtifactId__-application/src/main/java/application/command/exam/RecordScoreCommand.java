#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.command.exam;

public record RecordScoreCommand(String examId, String studentId, int points) {
}
