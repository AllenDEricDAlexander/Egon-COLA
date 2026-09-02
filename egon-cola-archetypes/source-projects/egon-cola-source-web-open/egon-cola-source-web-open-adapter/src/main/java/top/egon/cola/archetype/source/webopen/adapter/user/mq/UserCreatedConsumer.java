package top.egon.cola.archetype.source.webopen.adapter.user.mq;

import top.egon.cola.archetype.source.webopen.adapter.user.dto.CreateUserMessage;
import top.egon.cola.archetype.source.webopen.adapter.mq.OrganizationMessageSupport;
import top.egon.cola.archetype.source.webopen.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.webopen.application.user.manage.UserManage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCreatedConsumer {

    private final UserManage userManage;
    private final OrganizationMessageSupport messageSupport;

    @RabbitListener(
            queues = "student.organization.user.create.v1",
            autoStartup = "${organization.integrations.rabbit.enabled:false}")
    public void consume(CreateUserMessage message) {
        messageSupport.consume(() -> userManage.createUser(
                new CreateUserCommand(message.requestId(), message.name(), message.email())));
    }
}
