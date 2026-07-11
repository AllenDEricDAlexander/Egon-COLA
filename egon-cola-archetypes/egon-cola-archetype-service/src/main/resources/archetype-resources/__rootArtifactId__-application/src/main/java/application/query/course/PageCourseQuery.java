#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.query.course;

public record PageCourseQuery(int currentPage, int pageSize) {
}
