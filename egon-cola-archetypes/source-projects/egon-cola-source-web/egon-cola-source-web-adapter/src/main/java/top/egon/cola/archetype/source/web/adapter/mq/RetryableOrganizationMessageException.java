package top.egon.cola.archetype.source.web.adapter.mq;

public final class RetryableOrganizationMessageException extends RuntimeException {
    public RetryableOrganizationMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
