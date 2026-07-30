package top.egon.cola.platform.rbac3.contract.auth;

import java.util.Objects;

public record LoginRequest(
        String tenantCode,
        String username,
        String password,
        Device device
) {

    public LoginRequest {
        tenantCode = required(tenantCode, "tenantCode");
        username = required(username, "username");
        password = requiredSecret(password, "password");
        device = Objects.requireNonNull(device, "device");
    }

    @Override
    public String toString() {
        return "LoginRequest[tenantCode="
                + tenantCode
                + ", username="
                + username
                + ", password=<redacted>, device="
                + device
                + "]";
    }

    public record Device(
            String deviceId,
            String deviceName
    ) {

        public Device {
            deviceId = required(deviceId, "deviceId");
            deviceName = required(deviceName, "deviceName");
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String requiredSecret(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
