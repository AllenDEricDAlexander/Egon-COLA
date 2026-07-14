package top.egon.cola.component.methodextension.handler;

import java.util.Objects;

public final class MethodExtensionDecision {

    private final boolean allowed;

    private final boolean responseProvided;

    private final Object response;

    private final String reason;

    private MethodExtensionDecision(boolean allowed, boolean responseProvided, Object response, String reason) {
        this.allowed = allowed;
        this.responseProvided = responseProvided;
        this.response = response;
        this.reason = reason == null ? "" : reason;
    }

    public static MethodExtensionDecision allow() {
        return new MethodExtensionDecision(true, false, null, "");
    }

    public static MethodExtensionDecision reject() {
        return new MethodExtensionDecision(false, false, null, "");
    }

    public static MethodExtensionDecision rejectWithReason(String reason) {
        return new MethodExtensionDecision(false, false, null, reason);
    }

    public static MethodExtensionDecision reject(Object response) {
        return reject(response, "");
    }

    public static MethodExtensionDecision reject(Object response, String reason) {
        return new MethodExtensionDecision(
                false,
                true,
                Objects.requireNonNull(response, "response must not be null"),
                reason
        );
    }

    public boolean allowed() {
        return allowed;
    }

    public boolean responseProvided() {
        return responseProvided;
    }

    public Object response() {
        return response;
    }

    public String reason() {
        return reason;
    }
}
