package top.egon.cola.archetype.source.lightopen.adapter.user.mq;

import top.egon.cola.archetype.source.lightopen.adapter.user.validators.UserRequestValidator;
import top.egon.cola.archetype.source.lightopen.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.lightopen.application.user.manage.UserManage;
import top.egon.cola.archetype.source.lightopen.facade.user.dto.CreateUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserImportedConsumer {
    private final UserManage userManage;
    private final UserRequestValidator validator;

    @RabbitListener(
            queues = "${spring.application.name}.user.imported",
            errorHandler = "rabbitConsumerErrorHandler",
            autoStartup = "${app.integrations.rabbitmq.enabled:false}")
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
