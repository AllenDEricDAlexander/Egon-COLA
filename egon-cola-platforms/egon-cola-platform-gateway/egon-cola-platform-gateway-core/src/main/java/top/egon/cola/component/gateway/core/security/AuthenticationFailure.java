package top.egon.cola.component.gateway.core.security;

/**
 * Stable authentication failure categories used by credential recovery.
 */
public enum AuthenticationFailure {
    NONE,
    MISSING,
    EXPIRED,
    INVALID
}
