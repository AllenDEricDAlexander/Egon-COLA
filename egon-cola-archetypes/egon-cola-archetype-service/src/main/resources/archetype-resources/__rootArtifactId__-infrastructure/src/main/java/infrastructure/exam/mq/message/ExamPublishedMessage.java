#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.mq.message;
import java.time.Instant;
public record ExamPublishedMessage(
        Long examId, Long courseId, Long paperId, Instant publishedAt) { }
