package top.egon.cola.archetype.source.lightopen.adapter.handler;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.archetype.source.lightopen.common.exception.TeachingUseCaseException;
import top.egon.cola.archetype.source.lightopen.common.exception.UserUseCaseException;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;

@Component("rabbitConsumerErrorHandler")
@Slf4j
public class RabbitConsumerErrorHandler implements RabbitListenerErrorHandler {
    @Override
    public Object handleError(
            Message amqpMessage,
            Channel channel,
            org.springframework.messaging.Message<?> message,
            ListenerExecutionFailedException exception) throws Exception {
        Throwable cause = exception.getCause();
        if (cause instanceof IllegalArgumentException
                || cause instanceof UserUseCaseException
                || cause instanceof TeachingUseCaseException) {
            throw new AmqpRejectAndDontRequeueException("non-retryable consumer failure", cause);
        }
        throw exception;
    }
}
