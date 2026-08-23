#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.course.result;

public record CourseResult(long id, String code, String name, int credit, String status) {
}
