#set( $symbol_dollar = '$' )
package ${package}.adapter.mq.teaching;

import ${package}.adapter.dto.teaching.CreateSchoolClassMessage;
import ${package}.adapter.mq.OrganizationMessageSupport;
import ${package}.application.command.teaching.CreateSchoolClassCommand;
import ${package}.application.manage.teaching.SchoolClassManage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SchoolClassChangedConsumer {

    private final SchoolClassManage schoolClassManage;

    public SchoolClassChangedConsumer(SchoolClassManage schoolClassManage) {
        this.schoolClassManage = schoolClassManage;
    }

    @RabbitListener(
            queues = "student.organization.school-class.create.v1",
            autoStartup = "${symbol_dollar}{organization.integrations.rabbit.enabled:false}")
    public void consume(CreateSchoolClassMessage message) {
        OrganizationMessageSupport.consume(() -> schoolClassManage.createSchoolClass(
                new CreateSchoolClassCommand(message.requestId(), message.name(), message.gradeCode())));
    }
}
