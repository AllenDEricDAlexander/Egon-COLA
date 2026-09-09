package top.egon.cola.component.tianshu.service.refresh;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.api.refresh.DdcConfigApplier;

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
        registry.registerPrefix("yuheng.", (key, value, version) -> appliedBy.set("yuheng"));
        registry.registerPrefix("yuheng.route.", (key, value, version) -> appliedBy.set("route"));
        registry.registerExact("yuheng.route.primary", (key, value, version) -> appliedBy.set("exact"));
        registry.freeze();

        registry.resolve("yuheng.route.primary").apply("yuheng.route.primary", "value", 1L);
        assertThat(appliedBy).hasValue("exact");

        registry.resolve("yuheng.route.secondary").apply("yuheng.route.secondary", "value", 1L);
        assertThat(appliedBy).hasValue("route");

        registry.resolve("yuheng.timeout").apply("yuheng.timeout", "value", 1L);
        assertThat(appliedBy).hasValue("yuheng");

        registry.resolve("application.name").apply("application.name", "value", 1L);
        assertThat(appliedBy).hasValue("fallback");

        assertThat(registry.hasExplicitRegistration(
                "yuheng.route.primary"
        )).isTrue();
        assertThat(registry.hasExplicitRegistration(
                "yuheng.route.secondary"
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
        registry.registerExact("yuheng.route.primary", applier);
        registry.registerPrefix("yuheng.route.", applier);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerExact("yuheng.route.primary", applier))
                .withMessageContaining("already registered");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerPrefix("yuheng.route.", applier))
                .withMessageContaining("already registered");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerPrefix("yuheng.route", applier))
                .withMessageContaining("end with");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerExact(" ", applier));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerExact("yuheng.route.secondary", null));
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
                .isThrownBy(() -> registry.registerExact("yuheng.route.primary", applier))
                .withMessageContaining("frozen");
        assertThatIllegalStateException()
                .isThrownBy(() -> registry.registerPrefix("yuheng.route.", applier))
                .withMessageContaining("frozen");
    }
}
