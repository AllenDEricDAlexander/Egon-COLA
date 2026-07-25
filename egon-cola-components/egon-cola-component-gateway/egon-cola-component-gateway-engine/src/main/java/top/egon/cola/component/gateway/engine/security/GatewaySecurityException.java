package top.egon.cola.component.gateway.engine.security;

public final class GatewaySecurityException extends RuntimeException {

    private final String code;

    private final int httpStatus;

    private final String rpcStatus;

    public GatewaySecurityException(
            String code,
            int httpStatus,
            String rpcStatus) {
        super(code);
        this.code = code;
        this.httpStatus = httpStatus;
        this.rpcStatus = rpcStatus;
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String rpcStatus() {
        return rpcStatus;
    }

    public static GatewaySecurityException credentialInvalid() {
        return new GatewaySecurityException(
                "GATEWAY_CREDENTIAL_INVALID",
                401,
                "UNAUTHENTICATED"
        );
    }

    public static GatewaySecurityException authenticationRequired() {
        return new GatewaySecurityException(
                "GATEWAY_AUTHENTICATION_REQUIRED",
                401,
                "UNAUTHENTICATED"
        );
    }

    public static GatewaySecurityException authenticationFailed() {
        return new GatewaySecurityException(
                "GATEWAY_AUTHENTICATION_FAILED",
                401,
                "UNAUTHENTICATED"
        );
    }

    public static GatewaySecurityException authorizationDenied() {
        return new GatewaySecurityException(
                "GATEWAY_AUTHORIZATION_DENIED",
                403,
                "PERMISSION_DENIED"
        );
    }

    public static GatewaySecurityException providerTimeout() {
        return new GatewaySecurityException(
                "GATEWAY_SECURITY_PROVIDER_TIMEOUT",
                503,
                "UNAVAILABLE"
        );
    }

    public static GatewaySecurityException providerError() {
        return new GatewaySecurityException(
                "GATEWAY_SECURITY_PROVIDER_ERROR",
                503,
                "UNAVAILABLE"
        );
    }

    public static GatewaySecurityException identityMappingFailed() {
        return new GatewaySecurityException(
                "GATEWAY_IDENTITY_MAPPING_FAILED",
                500,
                "INTERNAL"
        );
    }
}
