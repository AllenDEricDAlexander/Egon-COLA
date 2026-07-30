package top.egon.cola.platform.rbac3.admin.application.port;

public interface RuntimeProjectionPort {

    ProjectionResult publish(RuntimeProjection projection);

    record RuntimeProjection(
            String tenantId,
            String scopeType,
            String scopeId,
            long version,
            String checksum,
            byte[] payload
    ) {
        public RuntimeProjection {
            payload = payload.clone();
        }
    }

    record ProjectionResult(boolean published, String reasonCode) {
    }
}
