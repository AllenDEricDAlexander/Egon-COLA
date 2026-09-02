package top.egon.cola.archetype.source.light.adapter.teaching.mq;

import top.egon.cola.archetype.source.light.adapter.teaching.validators.TeachingRequestValidator;
import top.egon.cola.archetype.source.light.application.teaching.command.CreateCourseCommand;
import top.egon.cola.archetype.source.light.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.light.facade.teaching.dto.CreateCourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseImportedConsumer {
    private final CourseManage courseManage;
    private final TeachingRequestValidator validator;

    @RabbitListener(
            queues = "${spring.application.name}.course.imported",
            errorHandler = "rabbitConsumerErrorHandler",
            autoStartup = "${app.integrations.rabbitmq.enabled:false}")
    public void consume(CreateCourseDTO message) {
        validator.validate(message);
        courseManage.create(new CreateCourseCommand(
                message.code(), message.name(), message.operatorId(), message.requestId()));
    }
}
