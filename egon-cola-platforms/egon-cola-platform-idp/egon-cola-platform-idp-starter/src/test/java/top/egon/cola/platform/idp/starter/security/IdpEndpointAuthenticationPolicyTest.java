package top.egon.cola.platform.idp.starter.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IdpEndpointAuthenticationPolicyTest {

    @Test
    void resolvesExplicitPublicAndServicePathsAndDefaultsApplicationsToUser() {
        IdpEndpointAuthenticationPolicy policy =
                new IdpEndpointAuthenticationPolicy(
                        List.of("/oauth2/login"),
                        List.of("/internal/**"));

        assertThat(policy.requirement(request("POST", "/oauth2/login")))
                .isEqualTo(IdpEndpointAuthenticationPolicy.Requirement.PUBLIC);
        assertThat(policy.requirement(request("POST", "/internal/v1/state")))
                .isEqualTo(IdpEndpointAuthenticationPolicy.Requirement.SERVICE);
        assertThat(policy.requirement(request("GET", "/api/v1/users")))
                .isEqualTo(IdpEndpointAuthenticationPolicy.Requirement.USER);
    }

    @Test
    void rejectsMissingEndpointPolicyInsteadOfSilentlyAllowingIt() {
        IdpEndpointAuthenticationPolicy policy =
                new IdpEndpointAuthenticationPolicy(
                        List.of(),
                        List.of(),
                        false);

        assertThat(policy.requirement(request("GET", "/unclassified")))
                .isEqualTo(IdpEndpointAuthenticationPolicy.Requirement.DENY);
    }

    @Test
    void classifiesInternalRefreshStatusAsServiceOnly() {
        IdpEndpointAuthenticationPolicy policy =
                new IdpEndpointAuthenticationPolicy(
                        List.of("/oauth2/token"),
                        List.of());

        assertThat(policy.requirement(request(
                "POST", "/internal/v1/oauth2/refresh-token/validate")))
                .isEqualTo(IdpEndpointAuthenticationPolicy.Requirement.SERVICE);
        assertThat(policy.requirement(request("POST", "/oauth2/token")))
                .isEqualTo(IdpEndpointAuthenticationPolicy.Requirement.PUBLIC);
    }

    private static MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }
}
