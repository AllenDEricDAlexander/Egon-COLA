package top.egon.cola.component.outbox.delivery.http;

import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.exception.OutboxValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

public class PropertiesHttpDestinationResolver implements HttpDestinationResolver {

    private final Map<String, HttpDeliveryTarget> targets;

    public PropertiesHttpDestinationResolver(TransactionalOutboxProperties properties) {
        Map<String, HttpDeliveryTarget> configured = new LinkedHashMap<>();
        properties.getHttp().getDestinations().forEach((name, destination) ->
                configured.put(name, new HttpDeliveryTarget(
                        destination.getUri(),
                        destination.getMethod(),
                        destination.getConnectTimeout(),
                        destination.getReadTimeout(),
                        destination.getFixedHeaders()
                )));
        this.targets = Map.copyOf(configured);
    }

    @Override
    public HttpDeliveryTarget resolve(String destination) {
        HttpDeliveryTarget target = targets.get(destination);
        if (target == null) {
            throw new OutboxValidationException(
                    "Unknown transactional outbox HTTP destination: " + destination
            );
        }
        return target;
    }
}
