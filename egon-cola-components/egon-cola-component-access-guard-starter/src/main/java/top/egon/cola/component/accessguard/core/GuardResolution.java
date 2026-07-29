package top.egon.cola.component.accessguard.core;

public enum GuardResolution {
    NONE,
    THROWN,
    FALLBACK,
    RETURN_JSON,
    RETURN_NULL,
    FAIL_OPEN,
    LOCAL_FALLBACK
}
