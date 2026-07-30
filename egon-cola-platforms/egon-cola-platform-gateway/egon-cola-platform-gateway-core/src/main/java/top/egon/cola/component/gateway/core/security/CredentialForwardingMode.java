package top.egon.cola.component.gateway.core.security;

/**
 * Controls whether a verified credential may cross the HTTP provider boundary.
 */
public enum CredentialForwardingMode {
    NONE,
    ORIGINAL_BEARER
}
