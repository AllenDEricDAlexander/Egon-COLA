package top.egon.cola.platform.idp.core.oauth;

public record AuthorizationRequest(
        String responseType,
        String clientId,
        String redirectUri,
        String audience,
        String tenantId,
        String state,
        String nonce,
        String codeChallenge,
        String codeChallengeMethod
) {

    public AuthorizationRequest withResponseType(String value) {
        return copy(
                value,
                clientId,
                redirectUri,
                audience,
                tenantId,
                state,
                nonce,
                codeChallenge,
                codeChallengeMethod
        );
    }

    public AuthorizationRequest withRedirectUri(String value) {
        return copy(
                responseType,
                clientId,
                value,
                audience,
                tenantId,
                state,
                nonce,
                codeChallenge,
                codeChallengeMethod
        );
    }

    public AuthorizationRequest withAudience(String value) {
        return copy(
                responseType,
                clientId,
                redirectUri,
                value,
                tenantId,
                state,
                nonce,
                codeChallenge,
                codeChallengeMethod
        );
    }

    public AuthorizationRequest withState(String value) {
        return copy(
                responseType,
                clientId,
                redirectUri,
                audience,
                tenantId,
                value,
                nonce,
                codeChallenge,
                codeChallengeMethod
        );
    }

    public AuthorizationRequest withNonce(String value) {
        return copy(
                responseType,
                clientId,
                redirectUri,
                audience,
                tenantId,
                state,
                value,
                codeChallenge,
                codeChallengeMethod
        );
    }

    public AuthorizationRequest withCodeChallengeMethod(String value) {
        return copy(
                responseType,
                clientId,
                redirectUri,
                audience,
                tenantId,
                state,
                nonce,
                codeChallenge,
                value
        );
    }

    private AuthorizationRequest copy(
            String newResponseType,
            String newClientId,
            String newRedirectUri,
            String newAudience,
            String newTenantId,
            String newState,
            String newNonce,
            String newCodeChallenge,
            String newCodeChallengeMethod
    ) {
        return new AuthorizationRequest(
                newResponseType,
                newClientId,
                newRedirectUri,
                newAudience,
                newTenantId,
                newState,
                newNonce,
                newCodeChallenge,
                newCodeChallengeMethod
        );
    }
}
