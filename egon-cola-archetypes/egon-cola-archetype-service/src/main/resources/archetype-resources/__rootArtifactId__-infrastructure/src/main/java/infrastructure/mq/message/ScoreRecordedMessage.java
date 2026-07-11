#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.mq.message;
public record ScoreRecordedMessage(
        String scoreId, String examId, String courseId, String studentId, int points) { }
