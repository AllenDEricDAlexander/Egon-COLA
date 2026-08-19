package top.egon.cola.component.outbox.serialization;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxMessageFingerprintTest {

    @Test
    void shouldIgnoreHeaderOrderAndSchedulingMetadata() {
        Map<String, String> first = new LinkedHashMap<>();
        first.put("X-B", "2");
        first.put("X-A", "1");
        Map<String, String> second = new LinkedHashMap<>();
        second.put("X-A", "1");
        second.put("X-B", "2");

        String left = OutboxMessageFingerprint.sha256(
                "http", "order-callback", "{}", "application/json", "1", first);
        String right = OutboxMessageFingerprint.sha256(
                "http", "order-callback", "{}", "application/json", "1", second);

        assertThat(left).isEqualTo(right).hasSize(64);
    }

    @Test
    void shouldChangeWhenPersistedContentChanges() {
        String left = OutboxMessageFingerprint.sha256(
                "http", "order-callback", "{\"v\":1}", "application/json", "1", Map.of());
        String right = OutboxMessageFingerprint.sha256(
                "http", "order-callback", "{\"v\":2}", "application/json", "1", Map.of());

        assertThat(left).isNotEqualTo(right);
    }
}
