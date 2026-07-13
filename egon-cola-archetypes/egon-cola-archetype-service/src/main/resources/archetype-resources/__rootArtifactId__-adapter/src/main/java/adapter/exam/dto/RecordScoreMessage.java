#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.dto;
import java.time.Instant;
public record RecordScoreMessage(
        String messageId, String examId, String studentId, int points, Instant occurredAt) { }
