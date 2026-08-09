package top.egon.cola.component.ddc.admin.service.lease;

import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;

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
                request.getBizCode(),
                request.getEnv(),
                request.getAppCode()
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
                request.getBizCode(),
                request.getEnv(),
                request.getAppCode()
        );
        if (isBlank(request.getLeaseId())) {
            throw invalid("leaseId is required");
        }
    }

    public void validateServiceRegistration(DdcServiceRegistration registration) {
        if (registration == null) {
            throw invalid("service registration is required");
        }
        if (registration.leaseSeconds() < minimumSeconds
                || registration.leaseSeconds() > maximumSeconds) {
            throw invalid(
                    "leaseSeconds must be between " + minimumSeconds + " and " + maximumSeconds
            );
        }
        if (registration.heartbeatIntervalSeconds() <= 0
                || registration.heartbeatIntervalSeconds() >= registration.leaseSeconds()) {
            throw invalid("heartbeatIntervalSeconds must be positive and less than leaseSeconds");
        }
    }

    private void validateIdentity(
            String instanceId, String bizCode, String env, String appCode) {
        if (isBlank(instanceId)) {
            throw invalid("instanceId is required");
        }
        if (isBlank(bizCode)) {
            throw invalid("bizCode is required");
        }
        if (isBlank(appCode)) {
            throw invalid("appCode is required");
        }
        if (isBlank(env)) {
            throw invalid("env is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private DdcAdminException invalid(String message) {
        return new DdcAdminException(message);
    }
}
