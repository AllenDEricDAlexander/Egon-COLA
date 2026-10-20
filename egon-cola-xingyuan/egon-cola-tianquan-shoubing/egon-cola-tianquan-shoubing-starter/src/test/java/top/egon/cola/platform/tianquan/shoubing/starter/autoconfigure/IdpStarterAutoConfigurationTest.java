package top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import io.grpc.ServerInterceptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.consumer.generic.RpcGenericInvocation;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInvocation;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInterceptorFactory;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.tianquan.shoubing.starter.client.ServiceTokenWebClientConfiguration;
import top.egon.cola.platform.tianquan.shoubing.starter.security.CurrentIdentity;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceScopeAuthorization;
import top.egon.cola.platform.tianquan.shoubing.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcBearerServerInterceptor;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcClientCredentialInterceptorFactory;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

class IdpStarterAutoConfigurationTest {

    @Test
    void explicitlyConfiguredAuditFailsClosedWhenHostDependenciesAreMissing() {
        new ApplicationContextRunner()
                .withUserConfiguration(IdpStarterAutoConfiguration.OAuthAuditConfiguration.class)
                .withPropertyValues(
                        "egon.cola.platform.tianquan.shoubing.audit.spool-directory=/tmp/owned-audit-fixture",
                        "egon.cola.platform.tianquan.shoubing.audit.hmac-key-file=/tmp/owned-audit-fixture/key")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("tokenUsageAuditComponent")
                            .hasStackTraceContaining("RedissonClient");
                });
    }

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            IdpStarterAutoConfiguration.class, IdpSecurityObservationAutoConfiguration.class))
                    .withBean("jacksonObjectMapper", ObjectMapper.class, ObjectMapper::new)
                    .withBean(RedissonClient.class,
                            () -> mock(RedissonClient.class))
                    .withPropertyValues(
                            "egon.cola.platform.tianquan.shoubing.enabled=true",
                            "egon.cola.platform.tianquan.shoubing.issuer=https://tianquan-shoubing.local",
                            "egon.cola.platform.tianquan.shoubing.jwk-set-uri=https://tianquan-shoubing.local/oauth2/jwks",
                            "egon.cola.platform.tianquan.shoubing.resource-server-id=resource-tianquan-jianshen-prod",
                            "egon.cola.platform.tianquan.shoubing.resource-uri=https://api.example/prod/permission/tianquan-jianshen");

    @Test
    void customNamedHostEndpointPolicyReplacesTheDefaultAndStillCreatesTheBearerFilter() {
        var policy = new top.egon.cola.platform.tianquan.shoubing.starter.security.IdpEndpointAuthenticationPolicy(
                List.of(), List.of("/internal/**"));
        contextRunner.withBean("customHostEndpointPolicy",
                        top.egon.cola.platform.tianquan.shoubing.starter.security.IdpEndpointAuthenticationPolicy.class,
                        () -> policy)
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean("idpEndpointAuthenticationPolicy")
                            .hasSingleBean(IdpBearerAuthenticationFilter.class);
                    assertThat(context.getBean(top.egon.cola.platform.tianquan.shoubing.starter.security.IdpEndpointAuthenticationPolicy.class))
                            .isSameAs(policy);
                });
    }

    @Test
    void configuredRpcAndOldOverloadsUseNamedDependenciesWithoutAServiceClientForProviders() throws Exception {
        IdpRpcClientCredentialInterceptorFactory.class.getConstructor();
        IdpRpcBearerServerInterceptor.class.getConstructor(UserAccessTokenVerifier.class);
        var compatibility = new IdpStarterAutoConfiguration();
        assertThat(compatibility.idpRpcClientCredentialInterceptorFactory()).isNotNull();
        assertThat(compatibility.idpRpcBearerServerInterceptor(mock(UserAccessTokenVerifier.class))).isNotNull();
        contextRunner.withBean("defaultValidator", Validator.class, LocalValidatorFactoryBean::new)
                .withPropertyValues(providerSettings()).run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(IdpServiceOAuth2Client.class);
                    var interceptor = context.getBean("idpRpcBearerServerInterceptor", IdpRpcBearerServerInterceptor.class);
                    @SuppressWarnings("unchecked") ServerCall<String, String> call = mock(ServerCall.class);
                    var method = MethodDescriptor.<String, String>newBuilder().setType(MethodDescriptor.MethodType.UNARY)
                            .setFullMethodName("example.Inventory/Read").setRequestMarshaller(mock(MethodDescriptor.Marshaller.class))
                            .setResponseMarshaller(mock(MethodDescriptor.Marshaller.class)).build();
                    when(call.getMethodDescriptor()).thenReturn(method);
                    @SuppressWarnings("unchecked") ServerCallHandler<String, String> next = mock(ServerCallHandler.class);
                    interceptor.interceptCall(call, new Metadata(), next);
                    verifyNoInteractions(next);
                    verify(call).close(org.mockito.ArgumentMatchers.argThat(status -> status.getCode() == Status.Code.UNAUTHENTICATED), any());
                });
    }

    @Test
    void configuredConsumerRejectsWrongApplicationOrEnvironmentBeforeTokenAcquisition() {
        var client = mock(IdpServiceOAuth2Client.class);
        contextRunner.withBean("defaultValidator", Validator.class, LocalValidatorFactoryBean::new)
                .withBean("idpServiceOAuth2Client", IdpServiceOAuth2Client.class, () -> client)
                .withPropertyValues(consumerSettings()).run(context -> {
                    assertThat(context).hasNotFailed();
                    var factory = context.getBean("idpRpcClientCredentialInterceptorFactory", IdpRpcClientCredentialInterceptorFactory.class);
                    var generic = RpcGenericInvocation.gateway("example.Inventory", "internal", "v1", "example.Inventory/Read",
                            new byte[]{1}, 1000, 0, LoadBalance.ROUND_ROBIN, FailStrategy.FAIL_CLOSED, null);
                    for (RpcProcessIdentity process : List.of(new RpcProcessIdentity("wrong-app", "test", "orders", 1, "test"),
                            new RpcProcessIdentity("orders", "wrong-env", "test", 1, "test"))) {
                        assertThatThrownBy(() -> factory.create(RpcClientInvocation.generic(generic, process, "call-1")))
                                .isInstanceOf(IllegalStateException.class);
                    }
                    verifyNoInteractions(client);
                });
    }

    @Test
    void enabledServiceConsumerNeedsItsActualOAuthClientAndValidatedBindings() {
        contextRunner.withBean("defaultValidator", Validator.class, LocalValidatorFactoryBean::new)
                .withPropertyValues(consumerSettings()).run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("SERVICE RPC requires idpServiceOAuth2Client");
                });
        contextRunner.withBean("defaultValidator", Validator.class, LocalValidatorFactoryBean::new)
                .withBean("idpServiceOAuth2Client", IdpServiceOAuth2Client.class, () -> mock(IdpServiceOAuth2Client.class))
                .withPropertyValues(consumerSettings()).withPropertyValues("egon.cola.platform.tianquan.shoubing.rpc.consumer.bindings.inventory.scopes[0]=*")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rpcUnknownKeysFailStrictBindingWithoutChangingObservationBinding() {
        contextRunner.withBean("defaultValidator", Validator.class, LocalValidatorFactoryBean::new)
                .withBean("idpServiceOAuth2Client", IdpServiceOAuth2Client.class, () -> mock(IdpServiceOAuth2Client.class))
                .withPropertyValues(consumerSettings())
                .withPropertyValues("egon.cola.platform.tianquan.shoubing.rpc.consumer.bindings.inventory.unknown-authority=grant")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("unknown-authority");
                });
    }

    private String[] consumerSettings() {
        String prefix = "egon.cola.platform.tianquan.shoubing.rpc.consumer.bindings.inventory.";
        return new String[]{"egon.cola.platform.tianquan.shoubing.rpc.enabled=true", "egon.cola.platform.tianquan.shoubing.rpc.consumer.enabled=true",
                prefix + "service-name=example.Inventory", prefix + "group=internal", prefix + "version=v1", prefix + "full-method-name=example.Inventory/Read",
                prefix + "process-identity.applicationName=orders", prefix + "process-identity.env=test", prefix + "mode=SERVICE",
                prefix + "registration-id=inventory-client", prefix + "app-id=orders-app", prefix + "resource-uri=https://api.example/inventory",
                prefix + "scope-context=PLATFORM", prefix + "scopes[0]=inventory:read"};
    }

    private String[] providerSettings() {
        String prefix = "egon.cola.platform.tianquan.shoubing.rpc.provider.methods[example.Inventory/Read].";
        return new String[]{"egon.cola.platform.tianquan.shoubing.rpc.enabled=true", "egon.cola.platform.tianquan.shoubing.rpc.provider.enabled=true",
                prefix + "required=true", prefix + "principal-kind=SERVICE", prefix + "scope-context=TENANT", prefix + "required-scopes[0]=inventory:read",
                prefix + "expected-resource-uri=https://api.example/inventory"};
    }

    @Test
    void registersServiceTokenConfigurationAfterItsBeanDependencies() throws IOException {
        String resource = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            String imports = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String autoConfiguration = ServiceTokenWebClientConfiguration.class.getName();
            assertThat(imports.lines().filter(autoConfiguration::equals).count())
                    .isEqualTo(1L);
        }
        AutoConfigureAfter order = ServiceTokenWebClientConfiguration.class
                .getAnnotation(AutoConfigureAfter.class);
        assertThat(order.value()).contains(
                IdpStarterAutoConfiguration.class,
                WebClientAutoConfiguration.class);
    }

    @Test
    void existingOverloadsAndNamedRedisStayCompatible() throws Exception {
        var type = top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent.class;
        var propertiesType = top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties.class;
        type.getConstructor(RedissonClient.class, ObjectMapper.class, propertiesType);
        var legacy = type.getConstructor(RedissonClient.class, ObjectMapper.class, propertiesType,
                top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties.class,
                java.time.Clock.class, jakarta.validation.Validator.class);
        var configured = type.getConstructor(RedissonClient.class, ObjectMapper.class, propertiesType,
                top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties.class,
                java.time.Clock.class, jakarta.validation.Validator.class,
                top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder.class);
        assertThat(legacy.getParameters()[0].getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value())
                .isEqualTo("rbac3RuntimeRedissonClient");
        assertThat(configured.getParameters()[0].getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value())
                .isEqualTo("idpAuditRedissonClient");
        var disabled = new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties(false, null, false);
        assertThat(disabled.isReceiverValid()).isTrue();
        var primary = mock(RedissonClient.class);
        contextRunner.withBean("rbac3RuntimeRedissonClient", RedissonClient.class, () -> primary)
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean("idpSecurityMetricsRecorder");
                    assertThat(context.getBean("idpAuditRedissonClient")).isSameAs(primary);
                });
        String resource = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            assertThat(new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(IdpSecurityObservationAutoConfiguration.class.getName()::equals).count()).isEqualTo(1);
        }
    }

    @Test
    void providesIdentityOnlyFilterBeforeRbac3Filter() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(
                    IdentityResourceServerStateReader.class);
            assertThat(context).hasSingleBean(
                    IdentityOAuthClientStateReader.class);
            assertThat(context).hasSingleBean(IdpJwtVerifier.class);
            assertThat(context).hasSingleBean(CurrentIdentity.class);
            assertThat(context).hasSingleBean(IdpBearerAuthenticationFilter.class);
            assertThat(context).hasSingleBean(ServiceScopeAuthorization.class);
            assertThat(context).hasSingleBean(
                    IdpRpcClientCredentialInterceptorFactory.class
            );
            assertThat(context).hasSingleBean(
                    IdpRpcBearerServerInterceptor.class
            );
            assertThat(context.getBeansOfType(
                    RpcClientInterceptorFactory.class
            ).values()).anyMatch(
                    IdpRpcClientCredentialInterceptorFactory.class::isInstance
            );
            assertThat(context.getBeansOfType(
                    ServerInterceptor.class
            ).values()).anyMatch(
                    IdpRpcBearerServerInterceptor.class::isInstance
            );
            FilterRegistrationBean<?> registration = context.getBean(
                    "idpBearerFilterRegistration",
                    FilterRegistrationBean.class);
            assertThat(registration.getOrder()).isEqualTo(-102);
        });
    }

    @Test
    void honorsRpcSecurityAdapterOverrides() {
        IdpRpcClientCredentialInterceptorFactory client =
                new IdpRpcClientCredentialInterceptorFactory();
        IdpRpcBearerServerInterceptor server =
                new IdpRpcBearerServerInterceptor(
                        mock(UserAccessTokenVerifier.class)
                );

        contextRunner
                .withBean(
                        IdpRpcClientCredentialInterceptorFactory.class,
                        () -> client
                )
                .withBean(
                        IdpRpcBearerServerInterceptor.class,
                        () -> server
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(
                            IdpRpcClientCredentialInterceptorFactory.class
                    );
                    assertThat(context).hasSingleBean(
                            IdpRpcBearerServerInterceptor.class
                    );
                    assertThat(context.getBean(
                            IdpRpcClientCredentialInterceptorFactory.class
                    )).isSameAs(client);
                    assertThat(context.getBean(
                            IdpRpcBearerServerInterceptor.class
                    )).isSameAs(server);
                });
    }

    @Test
    void remainsDisabledByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class, IdpSecurityObservationAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(IdpJwtVerifier.class);
                    assertThat(context).doesNotHaveBean(
                            IdpRpcClientCredentialInterceptorFactory.class
                    );
                    assertThat(context).doesNotHaveBean(
                            IdpRpcBearerServerInterceptor.class
                    );
                });
    }

    @Test
    void doesNotRequireServiceClientWhenOnlyResourceServerIsEnabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class, IdpSecurityObservationAutoConfiguration.class))
                .withBean("jacksonObjectMapper", ObjectMapper.class, ObjectMapper::new)
                .withBean(RedissonClient.class,
                        () -> mock(RedissonClient.class))
                .withPropertyValues(
                        "egon.cola.platform.tianquan.shoubing.enabled=true",
                        "egon.cola.platform.tianquan.shoubing.issuer=https://tianquan-shoubing.local",
                        "egon.cola.platform.tianquan.shoubing.jwk-set-uri=https://tianquan-shoubing.local/oauth2/jwks",
                        "egon.cola.platform.tianquan.shoubing.resource-server-id=resource-tianshu-local",
                        "egon.cola.platform.tianquan.shoubing.resource-uri=https://api.example/local/platform/tianshu",
                        "egon.cola.component.tianshu.enabled=false",
                        "egon.cola.component.tianshu.registry.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IdpJwtVerifier.class);
                    assertThat(context).doesNotHaveBean(
                            IdpServiceOAuth2Client.class);
                });
    }

    @Test
    void providesServiceClientWhenRegistrationIsConfigured() {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId("egon-tianquan-shoubing")
                .clientId("orders-key")
                .clientSecret("orders-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("https://tianquan-shoubing.local/oauth2/token")
                .issuerUri("https://tianquan-shoubing.local")
                .build();
        var serviceClientRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class,
                        IdpSecurityObservationAutoConfiguration.class,
                        ServiceTokenWebClientConfiguration.class))
                .withBean("jacksonObjectMapper", ObjectMapper.class, ObjectMapper::new)
                .withBean(RedissonClient.class,
                        () -> mock(RedissonClient.class))
                .withBean(ClientRegistrationRepository.class,
                        () -> new InMemoryClientRegistrationRepository(
                                registration))
                .withBean(WebClient.Builder.class, WebClient::builder)
                .withPropertyValues(
                        "egon.cola.platform.tianquan.shoubing.enabled=true",
                        "egon.cola.platform.tianquan.shoubing.issuer=https://tianquan-shoubing.local",
                        "egon.cola.platform.tianquan.shoubing.jwk-set-uri=https://tianquan-shoubing.local/oauth2/jwks",
                        "egon.cola.platform.tianquan.shoubing.resource-server-id=rs-tianquan-shoubing-prod",
                        "egon.cola.platform.tianquan.shoubing.resource-uri=https://api.example/tianquan-shoubing",
                        "egon.cola.platform.tianquan.shoubing.service-client.app-id=orders-app",
                        "egon.cola.platform.tianquan.shoubing.service-client.registration-id=egon-tianquan-shoubing",
                        "egon.cola.platform.tianquan.shoubing.service-client.credential-versions.egon-tianquan-shoubing=8");
        serviceClientRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(OAuth2AuthorizedClientManager.class);
            assertThat(context).hasSingleBean(IdpServiceOAuth2Client.class);
            assertThat(context).hasBean("serviceTokenWebClient");
            assertThat(context.getBean("idpStarterProperties", IdpStarterProperties.class)
                    .getServiceClient().getCredentialVersions()).containsEntry("egon-tianquan-shoubing", 8L);
        });
        var bootService = new org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService(
                new InMemoryClientRegistrationRepository(registration));
        serviceClientRunner.withBean("authorizedClientService",
                        org.springframework.security.oauth2.client.OAuth2AuthorizedClientService.class, () -> bootService)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(IdpServiceOAuth2Client.class);
                    assertThat(context).doesNotHaveBean("idpOAuth2AuthorizedClientService");
                    assertThat(context.getBean(org.springframework.security.oauth2.client.OAuth2AuthorizedClientService.class))
                            .isSameAs(bootService);
                });
    }

    @Test
    void rejectsLegacyPrivateKeyAndAdmissionProperties() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IdpStarterAutoConfiguration.class, IdpSecurityObservationAutoConfiguration.class))
                .withPropertyValues(
                        "egon.cola.platform.tianquan.shoubing.enabled=true",
                        "egon.cola.platform.tianquan.shoubing.admission.private-key-path=/run/secrets/tianquan-shoubing.pem")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasMessageContaining("migration")
                        .hasMessageContaining("admission.private-key-path")
                        .hasMessageNotContaining("/run/secrets/tianquan-shoubing.pem"));
    }

    @Test
    void decoderAcceptsAccessTokenTypeValidatedByIdpVerifier()
            throws Exception {
        RSAKey key = new RSAKeyGenerator(2048)
                .keyID("tianquan-shoubing-local")
                .algorithm(JWSAlgorithm.RS256)
                .generate();
        byte[] jwkSet = ("{\"keys\":["
                + key.toPublicJWK().toJSONString()
                + "]}").getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );
        server.createContext("/oauth2/jwks", exchange -> {
            exchange.getResponseHeaders().add(
                    "Content-Type",
                    "application/json"
            );
            exchange.sendResponseHeaders(200, jwkSet.length);
            exchange.getResponseBody().write(jwkSet);
            exchange.close();
        });
        server.start();
        try {
            String issuer = "http://127.0.0.1:" + server.getAddress().getPort();
            String resource = "https://api.example/local/permission/tianquan-jianshen";
            IdpStarterProperties properties = new IdpStarterProperties();
            properties.setEnabled(true);
            properties.setIssuer(issuer);
            properties.setJwkSetUri(issuer + "/oauth2/jwks");
            properties.setResourceServerId("permission-tianquan-jianshen-local");
            properties.setResourceUri(URI.create(resource));
            JwtDecoder decoder = new IdpStarterAutoConfiguration()
                    .idpJwtDecoder(properties);
            Instant now = Instant.now();
            SignedJWT token = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .type(new JOSEObjectType("at+jwt"))
                            .keyID("tianquan-shoubing-local")
                            .build(),
                    JWTClaimsSet.parse(java.util.Map.of("iss", issuer, "sub", "tianquan-shoubing-service",
                            "aud", List.of(resource), "iat", now.getEpochSecond(), "nbf", now.getEpochSecond(),
                            "exp", now.plusSeconds(60).getEpochSecond(), "jti", "token-1"))
            );
            token.sign(new RSASSASigner(key));

            assertThat(decoder.decode(token.serialize()).getSubject())
                    .isEqualTo("tianquan-shoubing-service");
        } finally {
            server.stop(0);
        }
    }

}
