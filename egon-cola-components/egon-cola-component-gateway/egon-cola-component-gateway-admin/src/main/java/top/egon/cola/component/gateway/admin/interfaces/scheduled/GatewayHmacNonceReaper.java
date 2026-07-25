package top.egon.cola.component.gateway.admin.interfaces.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayHmacNonceStore;

import java.time.Clock;

@Component
public class GatewayHmacNonceReaper {

    private final GatewayHmacNonceStore nonces;

    private final Clock clock = Clock.systemUTC();

    public GatewayHmacNonceReaper(GatewayHmacNonceStore nonces) {
        this.nonces = nonces;
    }

    @Scheduled(fixedDelayString = "${gateway.admin.hmac.reap-delay:PT5M}")
    public void reap() {
        nonces.deleteExpired(clock.instant());
    }
}
