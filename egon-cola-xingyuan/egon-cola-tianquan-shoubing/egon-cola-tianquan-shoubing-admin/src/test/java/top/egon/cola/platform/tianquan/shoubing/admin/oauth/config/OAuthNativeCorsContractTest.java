package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpSecurityConfig;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter.LegacyOAuthRetirementFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/** Exercises the existing SAS chain with its in-memory native protocol fixture. */
class OAuthNativeCorsContractTest {
    private static final String ORIGIN = "https://app.example.test";
    private static AnnotationConfigWebApplicationContext context;
    private static MockMvc mvc;

    @BeforeAll
    static void setUp() throws Exception {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                "egon.tianquan-shoubing.oauth.allowed-origins=" + ORIGIN);
        // Reuse the existing package-private fixture without widening its production/test API.
        context.register(Class.forName("top.egon.cola.platform.tianquan.shoubing.admin.oauth.OAuthConsentRoundTripTest$NativeConfiguration"), CorsConfiguration.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterAll
    static void close() { if (context != null) context.close(); }

    @Test
    void specializedSecurityChainRemainsReachableBeforeTheDenyAllFallback() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/unmatched-path")).andExpect(status().isForbidden());
    }

    @Test
    void allowsOnlyTheFourNativeResourcesWithoutCredentialsOrBody() throws Exception {
        for (String suffix : List.of("oauth2/token", "userinfo", ".well-known/openid-configuration", ".well-known/jwks.json")) {
            String method = suffix.equals("oauth2/token") ? "POST" : "GET";
            mvc.perform(preflight("/tenants/1001/" + suffix, ORIGIN, method))
                    .andExpect(status().isOk()).andExpect(content().string(""))
                    .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                    .andExpect(header().string("Access-Control-Allow-Methods", method))
                    .andExpect(header().string("Access-Control-Max-Age", "600"))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"))
                    .andExpect(header().doesNotExist("Set-Cookie"));
        }
    }

    @Test
    void rejectsUnlistedOriginsAndMethodsAndMalformedHeaders() throws Exception {
        for (String origin : List.of("null", "https://evil.example.test", "https://app.example.test.evil", "*", "https://app.example.test/", "https://app.example.test?x=y")) {
            reject(preflight("/tenants/1001/oauth2/token", origin, "POST"));
        }
        reject(preflight("/tenants/1001/oauth2/token", ORIGIN, "GET"));
        reject(preflight("/tenants/1001/userinfo", ORIGIN, "POST"));
        reject(preflight("/tenants/1001/oauth2/token", ORIGIN, "POST").header("Origin", ORIGIN));
        reject(preflight("/tenants/1001/oauth2/token", ORIGIN, "POST").header("Access-Control-Request-Method", "POST"));
        reject(preflight("/tenants/1001/oauth2/token", ORIGIN, "POST").header("Access-Control-Request-Headers", "authorization,authorization"));
        reject(preflight("/tenants/1001/oauth2/token", ORIGIN, "POST").header("Access-Control-Request-Headers", "x-tenant-id"));
        reject(preflight("/tenants/1001/oauth2/token", ORIGIN, "POST").header("Access-Control-Request-Headers", "authorization".repeat(100)));
        reject(preflight("/tenants/1001/oauth2/token", ORIGIN, "POST").header("Access-Control-Request-Headers", String.join(",", java.util.Collections.nCopies(17, "authorization"))));
        mvc.perform(preflight("/tenants/1001/oauth2/token", ORIGIN, "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Headers", "Authorization, Content-Type"));
    }

    @Test
    void rejectsMissingPreflightHeadersAndNonCanonicalTenantIds() throws Exception {
        reject(options("/tenants/1001/oauth2/token").header("Access-Control-Request-Method", "POST"));
        reject(options("/tenants/1001/oauth2/token").header("Origin", ORIGIN));
        for (String tenant : List.of("0", "01", "-1", "abc", "%2F", "..", "9223372036854775808")) {
            reject(preflight("/tenants/" + tenant + "/oauth2/token", ORIGIN, "POST"));
        }
    }

    @Test
    void doesNotOpenLoginConsentAuthorizePlatformOrAdministration() throws Exception {
        for (String path : List.of("/tenants/1001/login", "/tenants/1001/consent", "/tenants/1001/oauth2/authorize", "/platform/oauth2/token", "/api/v1/clients", "/oauth2/token")) {
            mvc.perform(preflight(path, ORIGIN, "POST"))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
        mvc.perform(post("/tenants/1001/login")).andExpect(status().isForbidden());
        mvc.perform(post("/tenants/1001/oauth2/authorize")).andExpect(status().isForbidden());
    }

    @Test
    void validatesOriginShapeAndBoundsUsingTheExistingValidator() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            OAuthCorsProperties properties = new OAuthCorsProperties();
            org.assertj.core.api.Assertions.assertThat(validator.validate(properties)).isEmpty();
            for (String value : List.of("*", "null", "https://a.example/", "https://a.example?q=1", "https://user@a.example", "http://remote.example", "https://例子.example")) {
                properties.setAllowedOrigins(List.of(value));
                org.assertj.core.api.Assertions.assertThat(validator.validate(properties)).as(value).isNotEmpty();
            }
            properties.setAllowedOrigins(java.util.Collections.nCopies(65, ORIGIN));
            org.assertj.core.api.Assertions.assertThat(validator.validate(properties)).isNotEmpty();
        }
    }

    private static MockHttpServletRequestBuilder preflight(String path, String origin, String method) {
        return options(path).header("Origin", origin).header("Access-Control-Request-Method", method);
    }

    private static void reject(MockHttpServletRequestBuilder request) throws Exception {
        mvc.perform(request).andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"))
                .andExpect(content().string("Invalid CORS request"));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(IdpSecurityConfig.class)
    static class CorsConfiguration {
        @Bean("fixtureOpenApiSecurity")
        @org.springframework.core.annotation.Order(100)
        org.springframework.security.web.SecurityFilterChain openApi(
                org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
            return http.securityMatcher("/v3/api-docs/**")
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                            new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)))
                    .build();
        }
        @Bean
        static org.springframework.beans.factory.config.BeanFactoryPostProcessor removeFixtureFallback() {
            return factory -> ((org.springframework.beans.factory.support.BeanDefinitionRegistry) factory)
                    .removeBeanDefinition("fixtureFallbackSecurity");
        }
        @Bean("legacyOAuthRetirementFilter")
        LegacyOAuthRetirementFilter retirement(com.fasterxml.jackson.databind.ObjectMapper mapper) {
            return new LegacyOAuthRetirementFilter(mapper);
        }
    }
}
