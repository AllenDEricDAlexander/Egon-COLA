package top.egon.cola.component.gateway.admin.application;

public class GatewayAdminIdempotencyConflictException
        extends RuntimeException {

    public GatewayAdminIdempotencyConflictException() {
        super("idempotency key was reused with a different payload");
    }
}
