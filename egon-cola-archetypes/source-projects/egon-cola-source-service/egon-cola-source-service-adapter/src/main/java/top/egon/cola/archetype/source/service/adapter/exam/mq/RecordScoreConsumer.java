package top.egon.cola.archetype.source.service.adapter.exam.mq;

import top.egon.cola.archetype.source.service.adapter.exam.pojo.dto.RecordScoreMessage;
import top.egon.cola.archetype.source.service.application.exam.pojo.command.RecordScoreCommand;
import top.egon.cola.archetype.source.service.application.exam.manage.ScoreManage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("recordScoreConsumer")
@ConditionalOnProperty(prefix = "app.integrations.rabbitmq", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RecordScoreConsumer {
    @Qualifier("scoreManage")
    private final ScoreManage scoreManage;

    @RabbitListener(
            queues = "${app.integrations.rabbitmq.score-command-queue}",
            autoStartup = "${app.integrations.rabbitmq.listener-auto-startup:false}")
    public void consume(RecordScoreMessage message) {
        scoreManage.record(new RecordScoreCommand(message.examId(), message.studentId(), message.points()));
    }
}
