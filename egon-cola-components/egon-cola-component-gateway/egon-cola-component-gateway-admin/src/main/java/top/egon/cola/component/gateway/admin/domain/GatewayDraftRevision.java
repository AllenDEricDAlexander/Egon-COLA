package top.egon.cola.component.gateway.admin.domain;

public final class GatewayDraftRevision {

    private long value;

    public GatewayDraftRevision(long value) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "revision must not be negative"
            );
        }
        this.value = value;
    }

    public long value() {
        return value;
    }

    public long advance(long expectedRevision) {
        if (expectedRevision != value) {
            throw new GatewayAdminRevisionConflictException(value);
        }
        return ++value;
    }
}
