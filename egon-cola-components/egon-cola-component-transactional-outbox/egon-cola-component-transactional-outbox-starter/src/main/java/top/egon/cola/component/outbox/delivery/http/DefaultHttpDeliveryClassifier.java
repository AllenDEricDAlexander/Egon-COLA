package top.egon.cola.component.outbox.delivery.http;

import org.springframework.http.HttpStatusCode;
import top.egon.cola.component.outbox.delivery.DeliveryResult;

public class DefaultHttpDeliveryClassifier implements HttpDeliveryClassifier {

    @Override
    public DeliveryResult classify(HttpStatusCode status) {
        int value = status.value();
        if (status.is2xxSuccessful()) {
            return DeliveryResult.success();
        }
        if (value == 408 || value == 425 || value == 429 || status.is5xxServerError()) {
            return DeliveryResult.retryableFailure("HTTP_" + value, Integer.toString(value));
        }
        return DeliveryResult.permanentFailure("HTTP_" + value, Integer.toString(value));
    }
}
