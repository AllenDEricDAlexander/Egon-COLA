package top.egon.cola.platform.tianquan.jianshen.admin.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockHttpServletRequest;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant.controller.filter.TenantContextFilter;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant.service.TenantContextResolver;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpEndpointAuthenticationPolicy;

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

    @Test
    void keepsTenantFilterOutOfTheServletContainerRegistration() {
        Rbac3AdminSecurityConfiguration configuration =
                new Rbac3AdminSecurityConfiguration();
        TenantContextFilter filter = configuration.tenantContextFilter(
                new TenantContextResolver()
        );

        FilterRegistrationBean<TenantContextFilter> registration =
                configuration.tenantContextFilterRegistration(filter);

        assertThat(registration.getFilter()).isSameAs(filter);
        assertThat(registration.isEnabled()).isFalse();
    }
}
