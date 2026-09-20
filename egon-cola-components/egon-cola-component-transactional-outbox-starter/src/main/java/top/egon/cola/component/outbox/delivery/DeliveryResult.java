package top.egon.cola.component.outbox.delivery;

import top.egon.cola.component.common.core.enums.EgonEnum;

public record DeliveryResult(Kind kind, String code, String message) {

    public enum Kind implements EgonEnum {
        SUCCESS(0, "SUCCESS"),
        RETRYABLE_FAILURE(1, "RETRYABLE_FAILURE"),
        PERMANENT_FAILURE(2, "PERMANENT_FAILURE");

        private final int code;
        private final String message;

        Kind(int code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }
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
