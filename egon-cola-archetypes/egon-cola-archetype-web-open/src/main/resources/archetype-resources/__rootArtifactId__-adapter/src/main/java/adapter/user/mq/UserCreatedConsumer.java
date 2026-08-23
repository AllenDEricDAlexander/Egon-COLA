#set( $symbol_dollar = '$' )
package ${package}.adapter.user.mq;

import ${package}.adapter.user.dto.CreateUserMessage;
import ${package}.adapter.mq.OrganizationMessageSupport;
import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.manage.UserManage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCreatedConsumer {

    private final UserManage userManage;

    @RabbitListener(
            queues = "student.organization.user.create.v1",
            autoStartup = "${symbol_dollar}{organization.integrations.rabbit.enabled:false}")
    public void consume(CreateUserMessage message) {
        OrganizationMessageSupport.consume(() -> userManage.createUser(
                new CreateUserCommand(message.requestId(), message.name(), message.email())));
    }
}
