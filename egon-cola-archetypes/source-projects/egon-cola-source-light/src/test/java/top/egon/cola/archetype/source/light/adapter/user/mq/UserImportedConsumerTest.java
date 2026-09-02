package top.egon.cola.archetype.source.light.adapter.user.mq;

import top.egon.cola.archetype.source.light.adapter.user.validators.UserRequestValidator;
import top.egon.cola.archetype.source.light.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.light.application.user.manage.UserManage;
import top.egon.cola.archetype.source.light.facade.user.dto.CreateUserDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserImportedConsumerTest {
    @Test
    void maps_message_actor_and_id_to_application_command() {
        UserManage userManage = mock(UserManage.class);
        UserImportedConsumer consumer = new UserImportedConsumer(userManage, new UserRequestValidator());
        consumer.consume(new CreateUserDTO("ext-1", "Mario", "mario@example.com", "actor-1", "message-1"));

        ArgumentCaptor<CreateUserCommand> captor = ArgumentCaptor.forClass(CreateUserCommand.class);
        verify(userManage).create(captor.capture());
        assertThat(captor.getValue().operatorId()).isEqualTo("actor-1");
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("message-1");
    }
}
