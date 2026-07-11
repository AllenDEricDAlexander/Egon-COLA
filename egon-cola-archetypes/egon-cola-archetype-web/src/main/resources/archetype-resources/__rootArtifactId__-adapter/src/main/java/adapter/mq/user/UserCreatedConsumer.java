#set( $symbol_dollar = '$' )
package ${package}.adapter.mq.user;

import ${package}.adapter.dto.user.CreateUserMessage;
import ${package}.adapter.mq.OrganizationMessageSupport;
import ${package}.application.command.user.CreateUserCommand;
import ${package}.application.manage.user.UserManage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedConsumer {

    private final UserManage userManage;

    public UserCreatedConsumer(UserManage userManage) {
        this.userManage = userManage;
    }

    @RabbitListener(
            queues = "student.organization.user.create.v1",
            autoStartup = "${symbol_dollar}{organization.integrations.rabbit.enabled:false}")
    public void consume(CreateUserMessage message) {
        OrganizationMessageSupport.consume(() -> userManage.createUser(
                new CreateUserCommand(message.requestId(), message.name(), message.email())));
    }
}
