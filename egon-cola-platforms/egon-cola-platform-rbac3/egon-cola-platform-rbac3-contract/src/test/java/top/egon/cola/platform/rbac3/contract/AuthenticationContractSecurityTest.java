package top.egon.cola.platform.rbac3.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;
import top.egon.cola.platform.rbac3.contract.auth.LoginResult;
import top.egon.cola.platform.rbac3.contract.auth.RefreshResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthenticationContractSecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginRequestToStringDoesNotExposePassword() {
        String password = "login-password-secret";
        LoginRequest request = loginRequest(password);

        assertFalse(request.toString().contains(password));
    }

    @Test
    void tokenResultToStringDoesNotExposeCredentials() {
        String accessToken = "access-token-secret";
        String refreshToken = "refresh-token-secret";
        LoginResult login = loginResult(accessToken, refreshToken);
        RefreshResult refresh = refreshResult(accessToken, refreshToken);

        assertFalse(login.toString().contains(accessToken));
        assertFalse(login.toString().contains(refreshToken));
        assertFalse(refresh.toString().contains(accessToken));
        assertFalse(refresh.toString().contains(refreshToken));
    }

    @Test
    void loginRequestPreservesExactPasswordCharacters() {
        String password = "  密码 passphrase  ";

        assertEquals(password, loginRequest(password).password());
    }

    @Test
    void loginFamiliesKeepExactJsonFieldsAndRoundTrip() throws Exception {
        LoginRequest request = loginRequest("密码-passphrase");
        LoginResult login = loginResult(
                "access-token-secret",
                "refresh-token-secret"
        );
        RefreshResult refresh = refreshResult(
                "refreshed-access-token-secret",
                "rotated-refresh-token-secret"
        );

        assertJsonFields(request, List.of(
                "tenantCode",
                "username",
                "password",
                "device"
        ));
        assertJsonFields(login, List.of(
                "tokenType",
                "accessToken",
                "expiresIn",
                "refreshToken",
                "refreshExpiresIn",
                "sessionId",
                "roleActivationRequired",
                "activationCandidateCount",
                "activationCandidatesUrl",
                "bootstrapRequired"
        ));
        assertJsonFields(refresh, List.of(
                "tokenType",
                "accessToken",
                "expiresIn",
                "refreshToken",
                "refreshExpiresIn",
                "sessionId",
                "authVersion",
                "sessionVersion",
                "policyVersion",
                "roleActivationRequired",
                "activationReasonCode",
                "bootstrapRequired"
        ));
        assertEquals(request, roundTrip(request, LoginRequest.class));
        assertEquals(login, roundTrip(login, LoginResult.class));
        assertEquals(refresh, roundTrip(refresh, RefreshResult.class));
    }

    private LoginRequest loginRequest(String password) {
        return new LoginRequest(
                "finance-cn",
                "zhangsan",
                password,
                new LoginRequest.Device(
                        "browser-installation-id",
                        "Chrome on macOS"
                )
        );
    }

    private LoginResult loginResult(
            String accessToken,
            String refreshToken) {
        return new LoginResult(
                "Bearer",
                accessToken,
                900L,
                refreshToken,
                604800L,
                "40001",
                true,
                2,
                "/api/rbac3/v1/auth/role-activation-candidates",
                false
        );
    }

    private RefreshResult refreshResult(
            String accessToken,
            String refreshToken) {
        return new RefreshResult(
                "Bearer",
                accessToken,
                900L,
                refreshToken,
                604800L,
                "40001",
                43L,
                3L,
                18L,
                false,
                null,
                true
        );
    }

    private void assertJsonFields(Object value, List<String> fields)
            throws Exception {
        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsBytes(value)
        );
        List<String> actual = new ArrayList<>();
        json.fieldNames().forEachRemaining(actual::add);
        assertEquals(fields, actual);
    }

    private <T> T roundTrip(Object value, Class<T> type) throws Exception {
        return objectMapper.readValue(
                objectMapper.writeValueAsBytes(value),
                type
        );
    }
}
