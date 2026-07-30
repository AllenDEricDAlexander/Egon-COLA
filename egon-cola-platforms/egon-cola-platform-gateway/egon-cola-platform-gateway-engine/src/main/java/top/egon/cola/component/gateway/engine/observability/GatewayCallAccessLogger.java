package top.egon.cola.component.gateway.engine.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;
import top.egon.cola.component.gateway.engine.http.logging.GatewayBodyLogEvent;

import java.util.Base64;

public final class GatewayCallAccessLogger
        implements GatewayCallCompletionListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger("gateway.call.access");

    private static final Logger BODY_LOGGER =
            LoggerFactory.getLogger("gateway.body.access");

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

    public void onBody(GatewayBodyLogEvent event) {
        byte[] sample = event.sample();
        BODY_LOGGER.info(
                "gateway_body direction={} contentType={} bytes={} "
                        + "metadataOnly={} truncated={} sampleBase64={}",
                event.direction(),
                event.contentType(),
                event.totalBytes(),
                event.metadataOnly(),
                event.totalBytes() > sample.length,
                event.metadataOnly() || sample.length == 0
                        ? ""
                        : Base64.getEncoder().encodeToString(sample)
        );
    }

    public void onWebSocketFrame(
            String direction,
            String frameType,
            long payloadBytes,
            boolean finalFragment) {
        BODY_LOGGER.info(
                "gateway_body direction={} contentType=websocket bytes={} "
                        + "metadataOnly=true frameType={} finalFragment={}",
                direction,
                payloadBytes,
                frameType,
                finalFragment
        );
    }
}
