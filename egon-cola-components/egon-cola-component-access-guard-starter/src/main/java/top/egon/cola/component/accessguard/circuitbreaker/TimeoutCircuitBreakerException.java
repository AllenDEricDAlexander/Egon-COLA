package top.egon.cola.component.accessguard.circuitbreaker;

public class TimeoutCircuitBreakerException extends RuntimeException {

    public TimeoutCircuitBreakerException(String message) {
        super(message);
    }

    public TimeoutCircuitBreakerException(String message, Throwable cause) {
        super(message, cause);
    }
}
