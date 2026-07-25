package top.egon.cola.component.gateway.admin.interfaces.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import top.egon.cola.component.gateway.admin.application.observability.GatewayCallEventIngestService;

public class GatewayObservabilityRetentionReaper {

    private final GatewayCallEventIngestService service;

    public GatewayObservabilityRetentionReaper(
            GatewayCallEventIngestService service) {
        this.service = service;
    }

    @Scheduled(
            fixedDelayString =
                    "${gateway.admin.observability.retention-reap-ms:3600000}"
    )
    public void reap() {
        service.purgeExpired();
    }
}
