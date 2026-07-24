package top.egon.cola.component.outbox.delivery.http;

import org.springframework.http.HttpStatusCode;
import top.egon.cola.component.outbox.delivery.DeliveryResult;

public interface HttpDeliveryClassifier {

    DeliveryResult classify(HttpStatusCode status);
}
