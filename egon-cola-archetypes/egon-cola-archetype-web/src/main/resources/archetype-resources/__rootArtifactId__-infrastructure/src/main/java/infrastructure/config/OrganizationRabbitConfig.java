package ${package}.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "organization.integrations.rabbit", name = "enabled", havingValue = "true")
public class OrganizationRabbitConfig {
    public static final String COMMAND_EXCHANGE = "student.organization.command.v1";
    public static final String EVENT_EXCHANGE = "student.organization.event.v1";
    public static final String DEAD_LETTER_EXCHANGE = "student.organization.dlx.v1";
    public static final String CREATE_USER_KEY = "organization.command.user.create.v1";
    public static final String CREATE_SCHOOL_CLASS_KEY = "organization.command.teaching.school-class.create.v1";

    @Bean public TopicExchange commandExchange() { return new TopicExchange(COMMAND_EXCHANGE, true, false); }
    @Bean public TopicExchange eventExchange() { return new TopicExchange(EVENT_EXCHANGE, true, false); }
    @Bean public TopicExchange deadLetterExchange() { return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false); }

    @Bean public Queue createUserQueue() {
        return QueueBuilder.durable("student.organization.user.create.v1")
            .deadLetterExchange(DEAD_LETTER_EXCHANGE).deadLetterRoutingKey("organization.dead.user.create.v1").build();
    }

    @Bean public Queue createSchoolClassQueue() {
        return QueueBuilder.durable("student.organization.school-class.create.v1")
            .deadLetterExchange(DEAD_LETTER_EXCHANGE)
            .deadLetterRoutingKey("organization.dead.teaching.school-class.create.v1").build();
    }

    @Bean public Queue createUserDeadLetterQueue() {
        return QueueBuilder.durable("student.organization.user.create.v1.dlq").build();
    }

    @Bean public Queue createSchoolClassDeadLetterQueue() {
        return QueueBuilder.durable("student.organization.school-class.create.v1.dlq").build();
    }

    @Bean public Binding createUserBinding() {
        return BindingBuilder.bind(createUserQueue()).to(commandExchange()).with(CREATE_USER_KEY);
    }

    @Bean public Binding createSchoolClassBinding() {
        return BindingBuilder.bind(createSchoolClassQueue()).to(commandExchange()).with(CREATE_SCHOOL_CLASS_KEY);
    }

    @Bean public Binding createUserDeadLetterBinding() {
        return BindingBuilder.bind(createUserDeadLetterQueue()).to(deadLetterExchange())
                .with("organization.dead.user.create.v1");
    }

    @Bean public Binding createSchoolClassDeadLetterBinding() {
        return BindingBuilder.bind(createSchoolClassDeadLetterQueue()).to(deadLetterExchange())
                .with("organization.dead.teaching.school-class.create.v1");
    }
}
