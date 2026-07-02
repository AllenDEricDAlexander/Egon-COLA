#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.view.course;

public record CourseView(String id, String name, int credit, String status) {
}
