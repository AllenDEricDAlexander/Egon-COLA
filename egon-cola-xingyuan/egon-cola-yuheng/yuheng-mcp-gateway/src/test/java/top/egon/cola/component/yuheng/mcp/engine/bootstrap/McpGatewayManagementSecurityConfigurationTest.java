package top.egon.cola.component.yuheng.mcp.engine.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.mcp.engine.bootstrap.config.McpGatewayManagementSecurityConfiguration;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class McpGatewayManagementSecurityConfigurationTest {

    @Test
    void exposesAnonymousProbesWithoutLoginRedirectOrSessionAndDeniesOtherManagementPaths() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class, WebMvcAutoConfiguration.class))
                .withUserConfiguration(McpGatewayManagementSecurityConfiguration.class, ProbeController.class)
                .withBean("idpSecurityAuditReady", java.util.function.BooleanSupplier.class, () -> () -> true)
                .withBean("oauthUsageObservationProperties", OAuthUsageObservationProperties.class,
                        () -> new OAuthUsageObservationProperties(true, URI.create("https://idp.example/tenants/1001"),
                                false, IdpSecurityObservationReceiverEnum.MCP))
                .withBean("idpStarterProperties", IdpStarterProperties.class,
                        McpGatewayManagementSecurityConfigurationTest::identity)
                .withBean("defaultValidator", jakarta.validation.Validator.class,
                        () -> jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator())
                .withPropertyValues("egon.cola.component.yuheng.mcp-engine.listener.tls.enabled=true",
                        "egon.cola.component.yuheng.mcp-engine.outbound.rpc-tls.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var mvc = MockMvcBuilders.webAppContextSetup(context)
                            .addFilters(context.getBean(FilterChainProxy.class)).build();
                    for (String path : new String[]{"/actuator/health", "/actuator/health/readiness", "/actuator/info"}) {
                        var result = mvc.perform(get(path)).andExpect(status().isOk()).andReturn();
                        assertThat(result.getResponse().getHeader("Location")).isNull();
                        assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
                        assertThat(result.getRequest().getSession(false)).isNull();
                    }
                    for (String path : new String[]{"/actuator/metrics", "/mcp/test", "/login", "/anything"}) {
                        var result = mvc.perform(get(path)).andExpect(status().isForbidden()).andReturn();
                        assertThat(result.getResponse().getHeader("Location")).isNull();
                        assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
                    }
                });
    }

    private static IdpStarterProperties identity() {
        var properties = new IdpStarterProperties();
        properties.setResourceServerId("controlled-resource");
        properties.setResourceUri(URI.create("https://resource.example"));
        properties.getRevocation().setPrimaryEndpoint("rediss://redis.example:6380");
        properties.getRevocation().setEpoch("controlled-epoch");
        var issuer = new IdpStarterProperties.TrustedIssuerSettings();
        issuer.setIssuer("https://idp.example/tenants/1001");
        issuer.setJwksUri("https://idp.example/tenants/1001/.well-known/jwks.json");
        issuer.setRealmKey("tenant:1001");
        issuer.setRealmType("TENANT");
        issuer.setTenantId("1001");
        properties.setTrustedIssuers(Map.of("controlled", issuer));
        return properties;
    }

    @RestController
    static class ProbeController {
        @GetMapping({"/actuator/health", "/actuator/health/readiness", "/actuator/info", "/actuator/metrics",
                "/mcp/test", "/login", "/anything"})
        Map<String, String> probe() {
            return Map.of("status", "UP");
        }
    }
}
