package top.egon.cola.component.ddc.service.refresh;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.api.refresh.DdcConfigApplier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class DefaultDdcConfigApplierRegistryTest {

    @Test
    void resolvesExactThenLongestPrefixThenFallback() {
        AtomicReference<String> appliedBy = new AtomicReference<>();
        DdcConfigApplier fallback = (key, value, version) -> appliedBy.set("fallback");
        DefaultDdcConfigApplierRegistry registry = new DefaultDdcConfigApplierRegistry(fallback);
        registry.registerPrefix("gateway.", (key, value, version) -> appliedBy.set("gateway"));
        registry.registerPrefix("gateway.route.", (key, value, version) -> appliedBy.set("route"));
        registry.registerExact("gateway.route.primary", (key, value, version) -> appliedBy.set("exact"));
        registry.freeze();

        registry.resolve("gateway.route.primary").apply("gateway.route.primary", "value", 1L);
        assertThat(appliedBy).hasValue("exact");

        registry.resolve("gateway.route.secondary").apply("gateway.route.secondary", "value", 1L);
        assertThat(appliedBy).hasValue("route");

        registry.resolve("gateway.timeout").apply("gateway.timeout", "value", 1L);
        assertThat(appliedBy).hasValue("gateway");

        registry.resolve("application.name").apply("application.name", "value", 1L);
        assertThat(appliedBy).hasValue("fallback");

        assertThat(registry.hasExplicitRegistration(
                "gateway.route.primary"
        )).isTrue();
        assertThat(registry.hasExplicitRegistration(
                "gateway.route.secondary"
        )).isTrue();
        assertThat(registry.hasExplicitRegistration(
                "application.name"
        )).isFalse();
    }

    @Test
    void rejectsDuplicateAndInvalidRegistrations() {
        DdcConfigApplier applier = (key, value, version) -> {
        };
        DefaultDdcConfigApplierRegistry registry = new DefaultDdcConfigApplierRegistry(applier);
        registry.registerExact("gateway.route.primary", applier);
        registry.registerPrefix("gateway.route.", applier);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerExact("gateway.route.primary", applier))
                .withMessageContaining("already registered");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerPrefix("gateway.route.", applier))
                .withMessageContaining("already registered");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerPrefix("gateway.route", applier))
                .withMessageContaining("end with");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerExact(" ", applier));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerExact("gateway.route.secondary", null));
    }

    @Test
    void freezeIsIdempotentAndBlocksLaterRegistrations() {
        DdcConfigApplier applier = (key, value, version) -> {
        };
        DefaultDdcConfigApplierRegistry registry = new DefaultDdcConfigApplierRegistry(applier);

        registry.freeze();
        registry.freeze();

        assertThat(registry.frozen()).isTrue();
        assertThatIllegalStateException()
                .isThrownBy(() -> registry.registerExact("gateway.route.primary", applier))
                .withMessageContaining("frozen");
        assertThatIllegalStateException()
                .isThrownBy(() -> registry.registerPrefix("gateway.route.", applier))
                .withMessageContaining("frozen");
    }
}
