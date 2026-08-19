package top.egon.cola.component.outbox.delivery.http;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatusCode;
import top.egon.cola.component.outbox.delivery.DeliveryResult;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultHttpDeliveryClassifierTest {

    private final DefaultHttpDeliveryClassifier classifier = new DefaultHttpDeliveryClassifier();

    @ParameterizedTest
    @CsvSource({
            "200,SUCCESS",
            "204,SUCCESS",
            "302,PERMANENT_FAILURE",
            "400,PERMANENT_FAILURE",
            "404,PERMANENT_FAILURE",
            "408,RETRYABLE_FAILURE",
            "425,RETRYABLE_FAILURE",
            "429,RETRYABLE_FAILURE",
            "500,RETRYABLE_FAILURE",
            "503,RETRYABLE_FAILURE"
    })
    void shouldClassifyHttpStatus(int status, DeliveryResult.Kind expected) {
        DeliveryResult result = classifier.classify(HttpStatusCode.valueOf(status));

        assertThat(result.kind()).isEqualTo(expected);
        if (expected == DeliveryResult.Kind.SUCCESS) {
            assertThat(result.code()).isNull();
        } else {
            assertThat(result.code()).isEqualTo("HTTP_" + status);
            assertThat(result.message()).isEqualTo(Integer.toString(status));
        }
    }
}
