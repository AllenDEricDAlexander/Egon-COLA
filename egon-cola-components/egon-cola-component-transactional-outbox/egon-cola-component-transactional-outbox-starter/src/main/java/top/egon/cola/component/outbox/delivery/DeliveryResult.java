package top.egon.cola.component.outbox.delivery;

public record DeliveryResult(Kind kind, String code, String message) {

    public enum Kind {
        SUCCESS,
        RETRYABLE_FAILURE,
        PERMANENT_FAILURE
    }

    public static DeliveryResult success() {
        return new DeliveryResult(Kind.SUCCESS, null, null);
    }

    public static DeliveryResult retryableFailure(String code, String message) {
        return new DeliveryResult(Kind.RETRYABLE_FAILURE, code, message);
    }

    public static DeliveryResult permanentFailure(String code, String message) {
        return new DeliveryResult(Kind.PERMANENT_FAILURE, code, message);
    }
}
