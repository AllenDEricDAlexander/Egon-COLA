#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.aggregates;

import ${package}.domain.exam.entities.Score;

public record ScoreAggregate(Score score) {
}
