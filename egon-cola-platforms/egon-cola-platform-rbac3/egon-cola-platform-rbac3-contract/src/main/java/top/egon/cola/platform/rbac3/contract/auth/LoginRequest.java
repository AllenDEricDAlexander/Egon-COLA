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
        password = required(password, "password");
        device = Objects.requireNonNull(device, "device");
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
}
