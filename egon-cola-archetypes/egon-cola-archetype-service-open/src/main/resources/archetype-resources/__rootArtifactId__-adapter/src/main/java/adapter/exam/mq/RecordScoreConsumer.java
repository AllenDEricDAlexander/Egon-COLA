#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.exam.mq;

import ${package}.adapter.exam.dto.RecordScoreMessage;
import ${package}.application.exam.command.RecordScoreCommand;
import ${package}.application.exam.manage.ScoreManage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.integrations.rabbitmq", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RecordScoreConsumer {
    private final ScoreManage scoreManage;

    @RabbitListener(
            queues = "${symbol_dollar}{app.integrations.rabbitmq.score-command-queue}",
            autoStartup = "${symbol_dollar}{app.integrations.rabbitmq.listener-auto-startup:false}")
    public void consume(RecordScoreMessage message) {
        scoreManage.record(new RecordScoreCommand(message.examId(), message.studentId(), message.points()));
    }
}
