package top.egon.cola.component.outbox.delivery;

public interface DeliveryHandler {

    String channel();

    void validateDestination(String destination);

    DeliveryResult deliver(DeliveryContext context) throws Exception;
}
