#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.mq.message;
import java.time.Instant;
public record ExamPublishedMessage(
        long examId, long courseId, long paperId, Instant publishedAt) { }
