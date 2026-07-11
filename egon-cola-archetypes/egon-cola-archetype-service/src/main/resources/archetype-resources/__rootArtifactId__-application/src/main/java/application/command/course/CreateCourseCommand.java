#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.command.course;

public record CreateCourseCommand(String code, String name, int credit) {
}
