package top.egon.cola.platform.idp.core.token;

public class TokenException extends RuntimeException {

    private final String oauthError;

    public TokenException(String oauthError) {
        super("token request is invalid");
        if (oauthError == null || oauthError.isBlank()) {
            throw new IllegalArgumentException("oauthError is required");
        }
        this.oauthError = oauthError;
    }

    public String oauthError() {
        return oauthError;
    }
}
