package top.egon.cola.archetype.source.web.adapter.user.mq;

import top.egon.cola.archetype.source.web.adapter.user.dto.CreateUserMessage;
import top.egon.cola.archetype.source.web.adapter.mq.OrganizationMessageSupport;
import top.egon.cola.archetype.source.web.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCreatedConsumer {

    private final UserManage userManage;

    @RabbitListener(
            queues = "student.organization.user.create.v1",
            autoStartup = "${organization.integrations.rabbit.enabled:false}")
    public void consume(CreateUserMessage message) {
        OrganizationMessageSupport.consume(() -> userManage.createUser(
                new CreateUserCommand(message.requestId(), message.name(), message.email())));
    }
}
