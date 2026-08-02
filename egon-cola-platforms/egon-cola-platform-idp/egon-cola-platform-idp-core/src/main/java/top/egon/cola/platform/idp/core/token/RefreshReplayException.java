package top.egon.cola.platform.idp.core.token;

public final class RefreshReplayException extends TokenException {

    public RefreshReplayException() {
        super("invalid_grant");
    }
}
