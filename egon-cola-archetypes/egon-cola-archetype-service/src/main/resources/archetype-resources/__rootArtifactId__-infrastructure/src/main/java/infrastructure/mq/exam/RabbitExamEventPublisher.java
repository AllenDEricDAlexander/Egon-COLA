#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.mq.exam;

import ${package}.domain.common.EvaluationPortException;
import ${package}.domain.entities.exam.Exam;
import ${package}.domain.entities.exam.ExamPaper;
import ${package}.domain.entities.exam.Score;
import ${package}.domain.event.exam.ExamEventPublisher;
import ${package}.infrastructure.mq.message.ExamPublishedMessage;
import ${package}.infrastructure.mq.message.ScoreRecordedMessage;
import java.time.Instant;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.integrations.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitExamEventPublisher implements ExamEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String examRoutingKey;
    private final String scoreRoutingKey;

    public RabbitExamEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${symbol_dollar}{app.integrations.rabbitmq.exchange}") String exchange,
            @Value("${symbol_dollar}{app.integrations.rabbitmq.exam-published-routing-key}") String examRoutingKey,
            @Value("${symbol_dollar}{app.integrations.rabbitmq.score-recorded-routing-key}") String scoreRoutingKey) {
        this.rabbitTemplate = rabbitTemplate; this.exchange = exchange;
        this.examRoutingKey = examRoutingKey; this.scoreRoutingKey = scoreRoutingKey;
    }

    public void examPublished(Exam exam, ExamPaper paper) {
        send(examRoutingKey, new ExamPublishedMessage(
                exam.getId().value(), exam.getCourseId().value(), paper.getId(), Instant.now()));
    }

    public void scoreRecorded(Score score) {
        send(scoreRoutingKey, new ScoreRecordedMessage(
                score.getId(), score.getExamId().value(), score.getCourseId().value(),
                score.getStudentId(), score.getPoints().value()));
    }

    private void send(String routingKey, Object message) {
        try { rabbitTemplate.convertAndSend(exchange, routingKey, message); }
        catch (AmqpException failure) {
            throw new EvaluationPortException("publish evaluation event", "rabbitmq publish failed", failure);
        }
    }
}
