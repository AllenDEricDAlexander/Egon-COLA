package top.egon.cola.platform.rbac3.contract.auth;

public enum SessionStatus {
    ACTIVE,
    LOGGED_OUT,
    REVOKED,
    EXPIRED,
    COMPROMISED
}
