#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.course.command;

public record CreateCourseCommand(String code, String name, int credit) {
}
