#set( $symbol_dollar = '$' )
package ${package}.adapter.teaching.mq;

import ${package}.adapter.teaching.dto.CreateSchoolClassMessage;
import ${package}.adapter.mq.OrganizationMessageSupport;
import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.manage.SchoolClassManage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchoolClassChangedConsumer {

    private final SchoolClassManage schoolClassManage;

    @RabbitListener(
            queues = "student.organization.school-class.create.v1",
            autoStartup = "${symbol_dollar}{organization.integrations.rabbit.enabled:false}")
    public void consume(CreateSchoolClassMessage message) {
        OrganizationMessageSupport.consume(() -> schoolClassManage.createSchoolClass(
                new CreateSchoolClassCommand(message.requestId(), message.name(), message.gradeCode())));
    }
}
