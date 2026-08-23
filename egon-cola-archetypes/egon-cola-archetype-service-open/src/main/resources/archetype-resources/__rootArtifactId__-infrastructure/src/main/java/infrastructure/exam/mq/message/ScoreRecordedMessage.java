#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.mq.message;
public record ScoreRecordedMessage(
        long scoreId, long examId, long courseId, long studentId, int points) { }
