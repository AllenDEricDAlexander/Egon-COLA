package top.egon.cola.platform.tianquan.shoubing.starter.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Validator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;

/** Adapts bounded security observations to the host's existing meter registry. */
@Slf4j
@RequiredArgsConstructor
public class IdpSecurityMetricsComponent implements IdpSecurityMetricsRecorder {
    @Qualifier("idpMeterRegistrySupplier") private final Supplier<MeterRegistry> registry;
    @Qualifier("defaultValidator") private final Validator validator;
    @Qualifier("oauthUsageObservationProperties") private final OAuthUsageObservationProperties usage;

    private final AtomicLong spoolBytes = new AtomicLong();
    private Gauge spoolGauge;
    private final Map<String, Consumer<SecurityMetricObservationBO>> strategies = Map.of(
            "jwt.verify.duration", this::timer, "jwks.cache.access", this::counter,
            "token.cache.access", this::counter, "token.acquire.duration", this::timer,
            "oauth.authentication", this::counter, "rbac.authorization", this::counter,
            "oauth.audit.delivery", this::counter, "receipt.age", this::timer, "spool.bytes", this::gauge);

    @Override
    public void record(SecurityMetricObservationBO observation) {
        new ValidationUtils(validator).validate(observation);
        new ValidationUtils(validator).validate(usage);
        strategies.get(observation.getSignal()).accept(observation);
    }

    private void timer(SecurityMetricObservationBO sample) {
        Timer.builder(sample.getSignal()).tags("receiver", usage.getReceiver().name(), "result", sample.getResult())
                .register(registry.get()).record(sample.getDuration());
    }

    private void counter(SecurityMetricObservationBO sample) {
        Counter.builder(sample.getSignal()).tags("receiver", usage.getReceiver().name(), "result", sample.getResult())
                .register(registry.get()).increment();
    }

    private synchronized void gauge(SecurityMetricObservationBO sample) {
        spoolBytes.set(sample.getValue());
        if (spoolGauge == null) {
            spoolGauge = Gauge.builder("spool.bytes", spoolBytes, AtomicLong::get)
                    .tags("receiver", usage.getReceiver().name(), "result", "CURRENT").register(registry.get());
        }
    }
}
