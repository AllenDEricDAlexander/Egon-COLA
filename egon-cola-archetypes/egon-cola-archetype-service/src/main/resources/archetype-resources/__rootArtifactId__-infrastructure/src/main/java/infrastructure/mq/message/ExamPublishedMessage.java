#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.mq.message;
import java.time.Instant;
public record ExamPublishedMessage(
        String examId, String courseId, String paperId, Instant publishedAt) { }
