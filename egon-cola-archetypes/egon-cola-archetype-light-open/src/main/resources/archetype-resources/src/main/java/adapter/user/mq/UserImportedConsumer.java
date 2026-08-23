#set( $symbol_dollar = '$' )
package ${package}.adapter.user.mq;

import ${package}.adapter.user.validators.UserRequestValidator;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.manage.UserManage;
import ${package}.facade.user.dto.CreateUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserImportedConsumer {
    private final UserManage userManage;
    private final UserRequestValidator validator;

    @RabbitListener(
            queues = "${symbol_dollar}{spring.application.name}.user.imported",
            errorHandler = "rabbitConsumerErrorHandler",
            autoStartup = "${symbol_dollar}{app.integrations.rabbitmq.enabled:false}")
    public void consume(CreateUserDTO message) {
        validator.validate(message);
        userManage.create(new CreateUserCommand(
                message.externalId(),
                message.name(),
                message.email(),
                message.operatorId(),
                message.requestId()));
    }
}
