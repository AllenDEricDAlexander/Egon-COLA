#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.exam.query;

public record PageScoreQuery(String examId, int currentPage, int pageSize) {
}
