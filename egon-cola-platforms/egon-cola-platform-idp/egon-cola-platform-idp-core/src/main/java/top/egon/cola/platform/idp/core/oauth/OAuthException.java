package top.egon.cola.platform.idp.core.oauth;

public final class OAuthException extends RuntimeException {

    private final String oauthError;

    public OAuthException(String oauthError, String message) {
        super(message);
        if (oauthError == null || oauthError.isBlank()) {
            throw new IllegalArgumentException("oauthError is required");
        }
        this.oauthError = oauthError;
    }

    public String oauthError() {
        return oauthError;
    }
}
