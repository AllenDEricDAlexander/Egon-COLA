#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.query.exam;

public record PageScoreQuery(String examId, int currentPage, int pageSize) {
}
