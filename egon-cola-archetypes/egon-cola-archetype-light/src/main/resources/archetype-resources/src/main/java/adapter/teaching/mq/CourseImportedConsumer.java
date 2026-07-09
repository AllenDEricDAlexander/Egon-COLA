#set( $symbol_dollar = '$' )
package ${package}.adapter.teaching.mq;

import ${package}.adapter.teaching.validators.TeachingRequestValidator;
import ${package}.application.teaching.command.CreateCourseCommand;
import ${package}.application.teaching.manage.CourseManage;
import ${package}.facade.teaching.dto.CreateCourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseImportedConsumer {
    private final CourseManage courseManage;
    private final TeachingRequestValidator validator;

    @RabbitListener(
            queues = "${symbol_dollar}{spring.application.name}.course.imported",
            errorHandler = "rabbitConsumerErrorHandler",
            autoStartup = "${symbol_dollar}{app.integrations.rabbitmq.enabled:false}")
    public void consume(CreateCourseDTO message) {
        validator.validate(message);
        courseManage.create(new CreateCourseCommand(
                message.code(), message.name(), message.operatorId(), message.requestId()));
    }
}
