package top.egon.cola.archetype.source.webopen.adapter.teaching.mq;

import top.egon.cola.archetype.source.webopen.adapter.teaching.dto.CreateSchoolClassMessage;
import top.egon.cola.archetype.source.webopen.adapter.mq.OrganizationMessageSupport;
import top.egon.cola.archetype.source.webopen.application.teaching.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.SchoolClassManage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchoolClassChangedConsumer {

    private final SchoolClassManage schoolClassManage;
    private final OrganizationMessageSupport messageSupport;

    @RabbitListener(
            queues = "student.organization.school-class.create.v1",
            autoStartup = "${organization.integrations.rabbit.enabled:false}")
    public void consume(CreateSchoolClassMessage message) {
        messageSupport.consume(() -> schoolClassManage.createSchoolClass(
                new CreateSchoolClassCommand(message.requestId(), message.name(), message.gradeCode())));
    }
}
