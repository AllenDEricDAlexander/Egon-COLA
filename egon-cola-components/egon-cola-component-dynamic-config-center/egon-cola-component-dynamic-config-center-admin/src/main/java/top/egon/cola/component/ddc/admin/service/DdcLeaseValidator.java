package top.egon.cola.component.ddc.admin.service;

import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;

public class DdcLeaseValidator {

    private final int minimumSeconds;

    private final int maximumSeconds;

    public DdcLeaseValidator() {
        this(new DdcAdminProperties());
    }

    public DdcLeaseValidator(DdcAdminProperties properties) {
        this.minimumSeconds = properties.getLease().getMinimumSeconds();
        this.maximumSeconds = properties.getLease().getMaximumSeconds();
    }

    public void validateRegistration(DdcInstanceRegisterRequest request) {
        if (request == null) {
            throw invalid("registration request is required");
        }
        validateIdentity(
                request.getInstanceId(),
                request.getAppCode(),
                request.getEnv(),
                request.getNamespace()
        );
        if (isBlank(request.getHost())) {
            throw invalid("host is required");
        }
        if (request.getLeaseSeconds() < minimumSeconds || request.getLeaseSeconds() > maximumSeconds) {
            throw invalid("leaseSeconds must be between " + minimumSeconds + " and " + maximumSeconds);
        }
        if (request.getHeartbeatIntervalSeconds() <= 0
                || request.getHeartbeatIntervalSeconds() >= request.getLeaseSeconds()) {
            throw invalid("heartbeatIntervalSeconds must be positive and less than leaseSeconds");
        }
    }

    public void validateOperation(DdcHeartbeatRequest request) {
        if (request == null) {
            throw invalid("lease operation request is required");
        }
        validateIdentity(
                request.getInstanceId(),
                request.getAppCode(),
                request.getEnv(),
                request.getNamespace()
        );
        if (isBlank(request.getLeaseId())) {
            throw invalid("leaseId is required");
        }
    }

    private void validateIdentity(String instanceId, String appCode, String env, String namespace) {
        if (isBlank(instanceId)) {
            throw invalid("instanceId is required");
        }
        if (isBlank(appCode)) {
            throw invalid("appCode is required");
        }
        if (isBlank(env)) {
            throw invalid("env is required");
        }
        if (isBlank(namespace)) {
            throw invalid("namespace is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private DdcAdminException invalid(String message) {
        return new DdcAdminException(message);
    }
}
