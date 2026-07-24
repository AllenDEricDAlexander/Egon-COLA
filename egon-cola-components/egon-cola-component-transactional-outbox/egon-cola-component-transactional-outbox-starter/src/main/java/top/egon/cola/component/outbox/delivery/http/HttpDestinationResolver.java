package top.egon.cola.component.outbox.delivery.http;

public interface HttpDestinationResolver {

    HttpDeliveryTarget resolve(String destination);
}
