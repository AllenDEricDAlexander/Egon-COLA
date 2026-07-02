#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.dto.course;

public record CourseDTO(String id, String name, int credit, String status) {
}
