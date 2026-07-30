package top.egon.cola.component.gateway.engine.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

public final class GatewayCallAccessLogger
        implements GatewayCallCompletionListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger("gateway.call.access");

    @Override
    public void onComplete(GatewayCallEventV1 event) {
        LOGGER.info(
                "gateway_call eventId={} traceId={} protocol={} zone={} "
                        + "group={} operation={} route={} result={} code={} "
                        + "durationMs={} attempts={}",
                event.eventId(),
                event.trace().traceId(),
                event.request().protocol(),
                event.request().accessZone(),
                event.routing().gatewayGroupId(),
                event.routing().operationId(),
                event.routing().routeId(),
                event.result().category(),
                event.result().gatewayErrorCode(),
                event.result().durationMs(),
                event.attempts().size()
        );
    }
}
