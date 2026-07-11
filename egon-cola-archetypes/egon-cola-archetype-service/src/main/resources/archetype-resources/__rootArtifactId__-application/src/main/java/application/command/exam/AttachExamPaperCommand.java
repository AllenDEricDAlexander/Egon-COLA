#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.command.exam;

public record AttachExamPaperCommand(String examId, String title, int totalPoints) {
}
