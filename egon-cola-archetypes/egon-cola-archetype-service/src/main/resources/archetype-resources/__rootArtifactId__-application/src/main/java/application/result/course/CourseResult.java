#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.result.course;

public record CourseResult(String id, String code, String name, int credit, String status) {
}
