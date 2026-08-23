#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.query;

public record PageScoreQuery(long examId, int currentPage, int pageSize) {
}
