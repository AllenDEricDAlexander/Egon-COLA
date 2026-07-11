#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.aggregates.exam;

import ${package}.domain.entities.exam.Score;

public record ScoreAggregate(Score score) {
}
