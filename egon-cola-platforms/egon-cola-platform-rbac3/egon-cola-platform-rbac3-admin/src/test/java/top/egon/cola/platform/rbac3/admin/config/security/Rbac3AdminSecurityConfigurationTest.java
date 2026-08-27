package top.egon.cola.platform.rbac3.admin.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import top.egon.cola.platform.idp.starter.security.IdpEndpointAuthenticationPolicy;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3AdminSecurityConfigurationTest {

    @Test
    void classifiesOpenApiDocumentsAsServiceOnly() {
        IdpEndpointAuthenticationPolicy policy =
                new Rbac3AdminSecurityConfiguration()
                        .rbac3EndpointAuthenticationPolicy();

        assertThat(policy.requirement(new MockHttpServletRequest(
                "GET",
                "/v3/api-docs/iam"
        ))).isEqualTo(IdpEndpointAuthenticationPolicy.Requirement.SERVICE);
        assertThat(policy.requirement(new MockHttpServletRequest(
                "GET",
                "/api/rbac3/v1/iam/roles"
        ))).isEqualTo(IdpEndpointAuthenticationPolicy.Requirement.USER);
    }
}
