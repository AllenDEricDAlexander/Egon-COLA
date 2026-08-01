package top.egon.cola.platform.rbac3.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.activation.ReplaceActiveRolesRequest;
import top.egon.cola.platform.rbac3.contract.auth.LoginRequest;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ContractCompatibilityMatrixTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginContractNeverSelectsOrActivatesARole() {
        List<String> fields = componentNames(LoginRequest.class);

        assertEquals(List.of("tenantCode", "username", "password", "device"), fields);
        assertFalse(fields.stream().anyMatch(name -> name.toLowerCase().contains("role")));
    }

    @Test
    void javascriptUnsafeIdsRemainLosslessDecimalStrings() throws Exception {
        String unsafeInJavascript = "9007199254740993";
        ReplaceActiveRolesRequest request = new ReplaceActiveRolesRequest(
                List.of(unsafeInJavascript), 7L);

        ReplaceActiveRolesRequest restored = objectMapper.readValue(
                objectMapper.writeValueAsBytes(request), ReplaceActiveRolesRequest.class);

        assertEquals(List.of(unsafeInJavascript), restored.roleIds());
        assertEquals(String.class,
                ReplaceActiveRolesRequest.class.getRecordComponents()[0]
                        .getGenericType() instanceof java.lang.reflect.ParameterizedType type
                        ? type.getActualTypeArguments()[0]
                        : null);
    }

    @Test
    void tokenClaimsStayIdentityAndVersionOnly() {
        List<String> fields = componentNames(Rbac3TokenClaims.class);

        assertEquals(List.of(
                "iss", "aud", "sub", "tid", "sid", "av", "sv", "pv",
                "jti", "iat", "nbf", "exp", "kid"), fields);
        assertFalse(fields.containsAll(List.of("roles", "permissions")));
    }

    private static List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
    }
}
