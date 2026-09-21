package top.egon.cola.archetype.source.light.infrastructure.teaching.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.light.domain.teaching.service.TeachingEventService;
import top.egon.cola.archetype.source.light.domain.teaching.vos.TeachingEvent;
import top.egon.cola.archetype.source.light.infrastructure.mq.MqMessageService;
import top.egon.cola.archetype.source.light.infrastructure.mq.MqRouteEnum;

/** Resolves the declared route for a teaching event and hands the message to the single MQ boundary. */
@Validated
@Service("teachingEventService")
@RequiredArgsConstructor
@Slf4j
public class TeachingEventServiceImpl implements TeachingEventService {
    @Qualifier("mqMessageService")
    private final MqMessageService mqMessageService;

    @Override
    public void publish(TeachingEvent event) {
        mqMessageService.publish(MqRouteEnum.resolveProducer(event.type()), event);
    }
}
