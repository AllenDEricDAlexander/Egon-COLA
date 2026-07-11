#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.mq;

import ${package}.infrastructure.config.RabbitMqConfiguration;
import ${package}.infrastructure.mq.course.RabbitCourseEventPublisher;
import ${package}.infrastructure.mq.exam.RabbitExamEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RabbitMqConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    RabbitMqConfiguration.class,
                    RabbitCourseEventPublisher.class,
                    RabbitExamEventPublisher.class)
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withPropertyValues(
                    "app.integrations.rabbitmq.enabled=true",
                    "app.integrations.rabbitmq.exchange=evaluation.events",
                    "app.integrations.rabbitmq.score-command-queue=evaluation.score.command",
                    "app.integrations.rabbitmq.score-command-routing-key=score.command",
                    "app.integrations.rabbitmq.course-scheduled-routing-key=course.scheduled",
                    "app.integrations.rabbitmq.exam-published-routing-key=exam.published",
                    "app.integrations.rabbitmq.score-recorded-routing-key=score.recorded");

    @Test
    void shouldCreateBasicTopologyAndPublishersWithoutBrokerConnection() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TopicExchange.class);
            assertThat(context).hasSingleBean(Queue.class);
            assertThat(context).hasSingleBean(Binding.class);
            assertThat(context).hasSingleBean(MessageConverter.class);
            assertThat(context).hasSingleBean(RabbitCourseEventPublisher.class);
            assertThat(context).hasSingleBean(RabbitExamEventPublisher.class);
        });
    }
}
