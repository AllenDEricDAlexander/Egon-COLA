#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.mq;

import ${package}.domain.course.vos.CourseId;
import ${package}.domain.exam.entities.Score;
import ${package}.domain.exam.enums.ScoreStatus;
import ${package}.domain.exam.vos.ExamId;
import ${package}.domain.exam.vos.ScoreValue;
import ${package}.infrastructure.exam.mq.message.ScoreRecordedMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitExamEventPublisherTest {
    @Test
    void shouldPublishScoreRecordedMessage() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitExamEventPublisher publisher = new RabbitExamEventPublisher(
                template, "evaluation.events", "exam.published", "score.recorded");
        Score score = new Score(7001L, new ExamId(4001L), new CourseId(1001L),
                6001L, new ScoreValue(90), ScoreStatus.RECORDED);
        publisher.scoreRecorded(score);
        verify(template).convertAndSend(eq("evaluation.events"), eq("score.recorded"),
                argThat((Object message) -> ((ScoreRecordedMessage) message).scoreId() == 7001L));
    }
}
