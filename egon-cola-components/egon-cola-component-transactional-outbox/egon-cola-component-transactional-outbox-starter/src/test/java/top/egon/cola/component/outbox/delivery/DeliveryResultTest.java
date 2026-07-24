package top.egon.cola.component.outbox.delivery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryResultTest {

    @Test
    void shouldExposeTypedDeliveryOutcomes() {
        assertThat(DeliveryResult.success().kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        assertThat(DeliveryResult.retryableFailure("HTTP_503", "unavailable").kind())
                .isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
        assertThat(DeliveryResult.permanentFailure("HTTP_400", "bad request").kind())
                .isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
    }
}
