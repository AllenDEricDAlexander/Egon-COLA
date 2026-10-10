package top.egon.cola.platform.tianquan.jianshen.admin.config.security;

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

/** Host adapter for the existing registry; receiver identity is fixed by this class. */
@Slf4j
@RequiredArgsConstructor
public class TianquanSecurityMetricsComponent implements IdpSecurityMetricsRecorder {
    @Qualifier("idpMeterRegistrySupplier") private final Supplier<MeterRegistry> registry;
    @Qualifier("defaultValidator") private final Validator validator;

    private static final String RECEIVER = "JIANSHEN";
    private final AtomicLong spoolBytes = new AtomicLong();
    private final Map<String, Consumer<SecurityMetricObservationBO>> strategies = Map.of(
            "jwt.verify.duration", this::timer,
            "jwks.cache.access", this::counter,
            "token.cache.access", this::counter,
            "token.acquire.duration", this::timer,
            "oauth.authentication", this::counter,
            "rbac.authorization", this::counter,
            "oauth.audit.delivery", this::counter,
            "receipt.age", this::timer,
            "spool.bytes", this::gauge);

    @Override
    public void record(SecurityMetricObservationBO observation) {
        new ValidationUtils(validator).validate(observation);
        strategies.get(observation.getSignal()).accept(observation);
    }

    private void timer(SecurityMetricObservationBO sample) {
        Timer.builder(sample.getSignal()).tags("receiver", RECEIVER, "result", sample.getResult())
                .register(registry.get()).record(sample.getDuration());
    }

    private void counter(SecurityMetricObservationBO sample) {
        Counter.builder(sample.getSignal()).tags("receiver", RECEIVER, "result", sample.getResult())
                .register(registry.get()).increment();
    }

    private void gauge(SecurityMetricObservationBO sample) {
        spoolBytes.set(sample.getValue());
        Gauge.builder("spool.bytes", spoolBytes, AtomicLong::get).tags("receiver", RECEIVER, "result", "CURRENT")
                .register(registry.get());
    }
}
