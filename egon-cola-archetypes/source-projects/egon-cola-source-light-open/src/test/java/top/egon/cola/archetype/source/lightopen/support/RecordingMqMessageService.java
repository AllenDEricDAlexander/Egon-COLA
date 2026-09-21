package top.egon.cola.archetype.source.lightopen.support;

import top.egon.cola.archetype.source.lightopen.infrastructure.mq.MqMessageService;
import top.egon.cola.archetype.source.lightopen.infrastructure.mq.MqRouteEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Local MQ boundary for fixtures: the production publisher needs a broker, while a regression test
 * only has to observe which declared route and payload the code under test selected.
 */
public final class RecordingMqMessageService implements MqMessageService {

    public record Publication(MqRouteEnum route, Object payload) { }

    private final List<Publication> publications = new ArrayList<>();

    @Override
    public void publish(MqRouteEnum route, Object payload) {
        publications.add(new Publication(route, payload));
    }

    public List<Publication> publications() {
        return List.copyOf(publications);
    }
}
