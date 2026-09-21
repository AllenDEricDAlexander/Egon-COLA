package top.egon.cola.archetype.source.web.adapter.teaching.mq;

import top.egon.cola.archetype.source.web.adapter.teaching.pojo.dto.CreateSchoolClassMessage;
import top.egon.cola.archetype.source.web.adapter.mq.OrganizationMessageSupport;
import top.egon.cola.archetype.source.web.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.manage.SchoolClassManage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

@Component("schoolClassChangedConsumer")
@RequiredArgsConstructor
@Slf4j
public class SchoolClassChangedConsumer {

    @Qualifier("schoolClassManage")
    private final SchoolClassManage schoolClassManage;

    @RabbitListener(
            queues = "student.organization.school-class.create.v1",
            autoStartup = "${organization.integrations.rabbit.enabled:false}")
    public void consume(CreateSchoolClassMessage message) {
        OrganizationMessageSupport.consume(() -> schoolClassManage.createSchoolClass(
                new CreateSchoolClassCommand(message.requestId(), message.name(), message.gradeCode())));
    }
}
