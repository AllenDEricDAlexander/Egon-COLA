#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.mq.message;
public record ScoreRecordedMessage(
        Long scoreId, Long examId, Long courseId, Long studentId, int points) { }
