package top.egon.cola.platform.idp.core.identity;

import top.egon.cola.platform.idp.contract.IdpErrorCode;

import java.util.Objects;

public final class IdentityException extends RuntimeException {

    private final IdpErrorCode code;

    public IdentityException(IdpErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public IdpErrorCode code() {
        return code;
    }
}
