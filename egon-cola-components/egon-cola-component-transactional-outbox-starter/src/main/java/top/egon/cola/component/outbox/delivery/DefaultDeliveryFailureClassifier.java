package top.egon.cola.component.outbox.delivery;

public class DefaultDeliveryFailureClassifier implements DeliveryFailureClassifier {

    @Override
    public DeliveryResult classify(Exception exception) {
        return DeliveryResult.retryableFailure(
                "OUTBOX_DELIVERY_EXCEPTION",
                exception.getClass().getSimpleName()
        );
    }
}
