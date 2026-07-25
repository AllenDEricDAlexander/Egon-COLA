package top.egon.cola.component.gateway.admin.domain;

public final class GatewayAdminRevisionConflictException
        extends RuntimeException {

    private final long currentRevision;

    public GatewayAdminRevisionConflictException(long currentRevision) {
        super("GATEWAY_ADMIN_REVISION_CONFLICT");
        this.currentRevision = currentRevision;
    }

    public long currentRevision() {
        return currentRevision;
    }
}
