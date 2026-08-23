package ${package}.adapter.mq;

public final class RetryableOrganizationMessageException extends RuntimeException {
    public RetryableOrganizationMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
