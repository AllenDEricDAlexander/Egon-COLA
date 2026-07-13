#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.command;

public record AttachExamPaperCommand(String examId, String title, int totalPoints) {
}
