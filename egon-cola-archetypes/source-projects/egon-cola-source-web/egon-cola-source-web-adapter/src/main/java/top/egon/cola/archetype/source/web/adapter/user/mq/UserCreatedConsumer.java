package top.egon.cola.archetype.source.web.adapter.user.mq;

import top.egon.cola.archetype.source.web.adapter.user.pojo.dto.CreateUserMessage;
import top.egon.cola.archetype.source.web.adapter.mq.OrganizationMessageSupport;
import top.egon.cola.archetype.source.web.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

@Component("userCreatedConsumer")
@RequiredArgsConstructor
@Slf4j
public class UserCreatedConsumer {

    @Qualifier("userManage")
    private final UserManage userManage;

    @RabbitListener(
            queues = "student.organization.user.create.v1",
            autoStartup = "${organization.integrations.rabbit.enabled:false}")
    public void consume(CreateUserMessage message) {
        OrganizationMessageSupport.consume(() -> userManage.createUser(
                new CreateUserCommand(message.requestId(), message.name(), message.email())));
    }
}
