package top.egon.cola.archetype.source.lightopen.adapter.teaching.mq;

import top.egon.cola.archetype.source.lightopen.adapter.teaching.validators.TeachingRequestValidator;
import top.egon.cola.archetype.source.lightopen.application.teaching.pojo.command.CreateCourseCommand;
import top.egon.cola.archetype.source.lightopen.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.lightopen.facade.teaching.dto.CreateCourseDTO;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CourseImportedConsumerTest {
    private static final jakarta.validation.ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidationFactory() {
        VALIDATORS.close();
    }

    @Test
    void maps_actor_and_message_id() {
        CourseManage manage = mock(CourseManage.class);
        CourseImportedConsumer consumer = new CourseImportedConsumer(manage,
                new TeachingRequestValidator(new ValidationUtils(VALIDATORS.getValidator())));
        consumer.consume(new CreateCourseDTO("MATH", "Math", "actor-1", "message-1"));
        ArgumentCaptor<CreateCourseCommand> captor = ArgumentCaptor.forClass(CreateCourseCommand.class);
        verify(manage).create(captor.capture());
        assertThat(captor.getValue().operatorId()).isEqualTo("actor-1");
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("message-1");
    }
}
