package top.egon.cola.platform.tianquan.shoubing.starter.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;

class IdpSecurityMetricsComponentTest {
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

    @BeforeEach void prepare() { validator.afterPropertiesSet(); }
    @AfterEach void close() { validator.close(); }

    @Test
    void recordsAllNineSignalsWithEveryFixedReceiverAndNoDynamicTags() {
        for (var receiver : IdpSecurityObservationReceiverEnum.values()) {
            var registry = new SimpleMeterRegistry();
            try {
                var usage = new OAuthUsageObservationProperties(true, URI.create("https://idp.example/tenants/1001"), false, receiver);
                var recorder = new IdpSecurityMetricsComponent(() -> registry, validator, usage);
                for (String signal : java.util.List.of("jwt.verify.duration", "token.acquire.duration", "receipt.age")) {
                    recorder.record(SecurityMetricObservationBO.builder().signal(signal)
                            .result(signal.equals("receipt.age") ? "CONFIRMED" : "SUCCESS").duration(Duration.ofMillis(1)).build());
                }
                for (String signal : java.util.List.of("jwks.cache.access", "token.cache.access")) {
                    recorder.record(SecurityMetricObservationBO.builder().signal(signal).result("HIT").build());
                }
                recorder.record(SecurityMetricObservationBO.builder().signal("oauth.authentication").result("VALID").build());
                recorder.record(SecurityMetricObservationBO.builder().signal("rbac.authorization").result("ALLOW").build());
                recorder.record(SecurityMetricObservationBO.builder().signal("oauth.audit.delivery").result("SENT").build());
                recorder.record(SecurityMetricObservationBO.builder().signal("spool.bytes").result("CURRENT").value(123).build());
                recorder.record(SecurityMetricObservationBO.builder().signal("spool.bytes").result("CURRENT").value(456).build());
                assertThat(registry.getMeters()).hasSize(9).allSatisfy(meter -> {
                    assertThat(meter.getId().getTag("receiver")).isEqualTo(receiver.name());
                    assertThat(meter.getId().getTags()).allSatisfy(tag -> assertThat(tag.getKey()).isIn("receiver", "result"));
                });
                assertThat(registry.get("jwt.verify.duration").timer().count()).isEqualTo(1);
                assertThat(registry.get("spool.bytes").gauge().value()).isEqualTo(456);
            } finally { registry.close(); }
        }
    }

    @Test
    void invalidSampleOrMissingReceiverCannotRegisterAnUnboundedMeter() {
        var registry = new SimpleMeterRegistry();
        try {
            var usage = new OAuthUsageObservationProperties(true, URI.create("https://idp.example/tenants/1001"), false,
                    IdpSecurityObservationReceiverEnum.TIANSHU);
            var recorder = new IdpSecurityMetricsComponent(() -> registry, validator, usage);
            assertThatThrownBy(() -> recorder.record(SecurityMetricObservationBO.builder().signal("tenant.1001")
                    .result("user-1").build())).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
            usage.setReceiver(null);
            assertThatThrownBy(() -> recorder.record(SecurityMetricObservationBO.builder().signal("oauth.authentication")
                    .result("VALID").build())).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
            assertThat(registry.getMeters()).isEmpty();
        } finally { registry.close(); }
    }
}
