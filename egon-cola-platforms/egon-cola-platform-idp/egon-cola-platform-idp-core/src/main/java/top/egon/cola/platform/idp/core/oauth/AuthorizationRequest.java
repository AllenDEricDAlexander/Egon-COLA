package top.egon.cola.platform.idp.core.oauth;

/**
 * OAuth Authorization Endpoint 的单 Resource 授权请求。
 * Single-Resource request received by the OAuth Authorization Endpoint.
 *
 * @param responseType 响应类型 / response type
 * @param clientId Client 标识 / Client identifier
 * @param redirectUri 回调地址 / redirect URI
 * @param resource RFC 8707 Resource Identifier / RFC 8707 Resource Identifier
 * @param tenantId 目标租户 / target tenant
 * @param state 浏览器状态防护值 / browser state protection value
 * @param nonce Token Nonce / token nonce
 * @param codeChallenge PKCE S256 Challenge / PKCE S256 challenge
 * @param codeChallengeMethod PKCE 方法 / PKCE method
 */
public record AuthorizationRequest(
        String responseType,
        String clientId,
        String redirectUri,
        String resource,
        String tenantId,
        String state,
        String nonce,
        String codeChallenge,
        String codeChallengeMethod) {

    /**
     * 替换响应类型。
     * Replaces the response type.
     *
     * @param value 新响应类型 / new response type
     * @return 请求副本 / request copy
     */
    public AuthorizationRequest withResponseType(String value) {
        return copy(value, clientId, redirectUri, resource, tenantId, state,
                nonce, codeChallenge, codeChallengeMethod);
    }

    /**
     * 替换回调地址。
     * Replaces the redirect URI.
     *
     * @param value 新回调地址 / new redirect URI
     * @return 请求副本 / request copy
     */
    public AuthorizationRequest withRedirectUri(String value) {
        return copy(responseType, clientId, value, resource, tenantId, state,
                nonce, codeChallenge, codeChallengeMethod);
    }

    /**
     * 替换目标 Resource。
     * Replaces the target Resource.
     *
     * @param value 新 Resource / new Resource
     * @return 请求副本 / request copy
     */
    public AuthorizationRequest withResource(String value) {
        return copy(responseType, clientId, redirectUri, value, tenantId, state,
                nonce, codeChallenge, codeChallengeMethod);
    }

    /**
     * 替换 State。
     * Replaces the state value.
     *
     * @param value 新 State / new state
     * @return 请求副本 / request copy
     */
    public AuthorizationRequest withState(String value) {
        return copy(responseType, clientId, redirectUri, resource, tenantId,
                value, nonce, codeChallenge, codeChallengeMethod);
    }

    /**
     * 替换 Nonce。
     * Replaces the nonce.
     *
     * @param value 新 Nonce / new nonce
     * @return 请求副本 / request copy
     */
    public AuthorizationRequest withNonce(String value) {
        return copy(responseType, clientId, redirectUri, resource, tenantId,
                state, value, codeChallenge, codeChallengeMethod);
    }

    /**
     * 替换 PKCE Challenge 方法。
     * Replaces the PKCE challenge method.
     *
     * @param value 新 PKCE 方法 / new PKCE method
     * @return 请求副本 / request copy
     */
    public AuthorizationRequest withCodeChallengeMethod(String value) {
        return copy(responseType, clientId, redirectUri, resource, tenantId,
                state, nonce, codeChallenge, value);
    }

    /**
     * 创建字段替换后的请求副本。
     * Creates a request copy with replaced fields.
     *
     * @param newResponseType 新响应类型 / new response type
     * @param newClientId 新 Client 标识 / new Client identifier
     * @param newRedirectUri 新回调地址 / new redirect URI
     * @param newResource 新 Resource / new Resource
     * @param newTenantId 新租户 / new tenant
     * @param newState 新 State / new state
     * @param newNonce 新 Nonce / new nonce
     * @param newCodeChallenge 新 PKCE Challenge / new PKCE challenge
     * @param newCodeChallengeMethod 新 PKCE 方法 / new PKCE method
     * @return 请求副本 / request copy
     */
    private AuthorizationRequest copy(
            String newResponseType, String newClientId, String newRedirectUri,
            String newResource, String newTenantId, String newState,
            String newNonce, String newCodeChallenge,
            String newCodeChallengeMethod) {
        return new AuthorizationRequest(newResponseType, newClientId,
                newRedirectUri, newResource, newTenantId, newState, newNonce,
                newCodeChallenge, newCodeChallengeMethod);
    }
}
