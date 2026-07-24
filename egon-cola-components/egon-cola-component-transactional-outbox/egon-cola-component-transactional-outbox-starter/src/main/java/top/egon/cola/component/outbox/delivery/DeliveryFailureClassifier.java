package top.egon.cola.component.outbox.delivery;

public interface DeliveryFailureClassifier {

    DeliveryResult classify(Exception exception);
}
