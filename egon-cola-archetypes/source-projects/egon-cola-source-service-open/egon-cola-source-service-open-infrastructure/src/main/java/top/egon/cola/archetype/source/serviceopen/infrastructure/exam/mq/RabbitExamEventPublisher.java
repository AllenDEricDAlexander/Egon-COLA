package top.egon.cola.archetype.source.serviceopen.infrastructure.exam.mq;

import top.egon.cola.archetype.source.serviceopen.domain.common.EvaluationPortException;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Exam;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.ExamPaper;
import top.egon.cola.archetype.source.serviceopen.domain.exam.entities.Score;
import top.egon.cola.archetype.source.serviceopen.domain.exam.event.ExamEventPublisher;
import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.mq.message.ExamPublishedMessage;
import top.egon.cola.archetype.source.serviceopen.infrastructure.exam.mq.message.ScoreRecordedMessage;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.integrations.rabbitmq", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RabbitExamEventPublisher implements ExamEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    @Value("${app.integrations.rabbitmq.exchange}")
    private final String exchange;
    @Value("${app.integrations.rabbitmq.exam-published-routing-key}")
    private final String examRoutingKey;
    @Value("${app.integrations.rabbitmq.score-recorded-routing-key}")
    private final String scoreRoutingKey;

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
