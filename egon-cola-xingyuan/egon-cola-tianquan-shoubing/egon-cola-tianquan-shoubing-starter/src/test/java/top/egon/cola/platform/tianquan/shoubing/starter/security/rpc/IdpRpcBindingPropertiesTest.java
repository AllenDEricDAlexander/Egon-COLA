package top.egon.cola.platform.tianquan.shoubing.starter.security.rpc;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInvocation;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.descriptor.RpcMethodDescriptor;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import java.time.Instant;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcConsumerBindingProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.RpcConsumerModeEnum;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class IdpRpcBindingPropertiesTest {
    static final String SERVICE = "example.Inventory";
    static final String METHOD = "example.Inventory/Read";
    static final URI RESOURCE = URI.create("https://api.example/inventory");
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();

    @BeforeEach void prepare() { validator.afterPropertiesSet(); }
    @AfterEach void close() { validator.close(); }

    @Test
    void bindsRealBootPropertiesAndValidatesTheWholeNestedRpcContract() {
        String prefix = "egon.cola.platform.tianquan.shoubing.rpc.";
        var values = new LinkedHashMap<String, Object>();
        values.put(prefix + "enabled", true);
        values.put(prefix + "consumer.enabled", true);
        String binding = prefix + "consumer.bindings.inventory.";
        values.put(binding + "service-name", SERVICE);
        values.put(binding + "group", "internal");
        values.put(binding + "version", "v1");
        values.put(binding + "full-method-name", METHOD);
        values.put(binding + "process-identity.applicationName", "orders");
        values.put(binding + "process-identity.env", "test");
        values.put(binding + "mode", "SERVICE");
        values.put(binding + "registration-id", "inventory-client");
        values.put(binding + "app-id", "orders-app");
        values.put(binding + "resource-uri", RESOURCE.toString());
        values.put(binding + "scope-context", "PLATFORM");
        values.put(binding + "scopes[0]", "inventory:read");
        var properties = new Binder(new MapConfigurationPropertySource(values))
                .bind("egon.cola.platform.tianquan.shoubing", Bindable.of(IdpStarterProperties.class)).get();
        assertThat(validator.validate(properties)).isEmpty();
        assertThat(properties.getRpc().getConsumer().getBindings().get("inventory").getProcessIdentity())
                .containsExactlyInAnyOrderEntriesOf(Map.of("applicationName", "orders", "env", "test"));
    }

    @Test
    void duplicateSelectorsRejectRegardlessOfMapInsertionOrder() {
        var properties = properties(ServiceTokenContext.PLATFORM);
        var duplicate = binding(ServiceTokenContext.PLATFORM);
        var selectors = new LinkedHashMap<String, String>();
        selectors.put("env", "test");
        selectors.put("applicationName", "orders");
        duplicate.setProcessIdentity(selectors);
        properties.getRpc().getConsumer().getBindings().put("duplicate", duplicate);
        assertThat(validator.validate(properties)).anyMatch(value -> value.getPropertyPath().toString().endsWith("configurationValid"));
    }

    @Test
    void invalidScopesHttpsTenantMappingsAndProcessKeysCannotEnterRpc() {
        for (String invalid : List.of("scope with space", "*", "x".repeat(129))) {
            var binding = binding(ServiceTokenContext.PLATFORM).setScopes(Set.of(invalid));
            assertThat(validator.validate(binding)).isNotEmpty();
        }
        assertThat(validator.validate(binding(ServiceTokenContext.PLATFORM).setResourceUri(URI.create("http://api.example/inventory")))).isNotEmpty();
        assertThat(validator.validate(binding(ServiceTokenContext.TENANT).setTenantRegistrationIds(Map.of()))).isNotEmpty();
        assertThat(validator.validate(binding(ServiceTokenContext.PLATFORM).setTenantRegistrationIds(null))).isNotEmpty();
        assertThat(validator.validate(binding(ServiceTokenContext.PLATFORM).setProcessIdentity(Map.of("applicationName", "orders", "host", "test")))).isNotEmpty();
        assertThat(validator.validate(binding(ServiceTokenContext.PLATFORM).setScopes(Set.of()))).isNotEmpty();
    }

    @Test
    void stableProcessSelectorsAndUnknownTenantRejectBeforeAuthorize() throws Exception {
        var client = mock(IdpServiceOAuth2Client.class);
        var factory = IdpRpcClientCredentialInterceptorFactory.createConfigured(client, properties(ServiceTokenContext.PLATFORM), validator);
        assertThatThrownBy(() -> factory.create(invocation(new RpcProcessIdentity("other-app", "test", "orders", 1, "test"))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> factory.create(invocation(new RpcProcessIdentity("orders", "other-env", "test", 1, "test"))))
                .isInstanceOf(IllegalStateException.class);
        var tenantFactory = IdpRpcClientCredentialInterceptorFactory.createConfigured(client, properties(ServiceTokenContext.TENANT), validator);
        assertThatThrownBy(() -> tenantFactory.create(invocation(process())))
                .isInstanceOf(IllegalStateException.class);
        var now = Instant.parse("2026-10-11T00:00:00Z");
        var otherTenant = new IdentityPrincipal("user-2", "tenant-2", "jti-2", Set.of(RESOURCE.toASCIIString()), now,
                now.plusSeconds(300), AuthenticationContext.password());
        var invocation = invocation(process());
        IdpRpcSecurityContext.with(otherTenant, "verified-user").run(() ->
                assertThatThrownBy(() -> tenantFactory.create(invocation)).isInstanceOf(IllegalStateException.class));
        verifyNoInteractions(client);
    }

    static IdpStarterProperties properties(ServiceTokenContext context) {
        var properties = new IdpStarterProperties();
        properties.getRpc().setEnabled(true);
        properties.getRpc().getConsumer().setEnabled(true);
        properties.getRpc().getConsumer().getBindings().put("inventory", binding(context));
        return properties;
    }

    static RpcConsumerBindingProperties binding(ServiceTokenContext context) {
        return RpcConsumerBindingProperties.builder().serviceName(SERVICE).group("internal").version("v1")
                .fullMethodName(METHOD).processIdentity(Map.of("applicationName", "orders", "env", "test"))
                .mode(RpcConsumerModeEnum.SERVICE).registrationId(context == ServiceTokenContext.PLATFORM ? "inventory-client" : null)
                .tenantRegistrationIds(context == ServiceTokenContext.TENANT ? Map.of("tenant-1", "tenant-inventory-client") : Map.of())
                .appId("orders-app").resourceUri(RESOURCE).scopeContext(context).scopes(Set.of("inventory:read")).build();
    }

    static RpcProcessIdentity process() { return new RpcProcessIdentity("orders", "test", "localhost", 42, "instance-1"); }

    static RpcClientInvocation invocation(RpcProcessIdentity process) throws Exception {
        var javaMethod = TestRpcContract.class.getMethod("read", Empty.class);
        var grpcMethod = MethodDescriptor.<Message, Message>newBuilder().setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(METHOD).setRequestMarshaller(ProtoUtils.<Message>marshaller(Empty.getDefaultInstance()))
                .setResponseMarshaller(ProtoUtils.<Message>marshaller(Empty.getDefaultInstance())).build();
        var method = new RpcMethodDescriptor(javaMethod, "Read", METHOD, true, grpcMethod, null);
        var contract = new RpcContractDescriptor(TestRpcContract.class, SERVICE, "internal", "v1", List.of(method));
        return new RpcClientInvocation(contract, method, Empty.getDefaultInstance(), process);
    }

    public interface TestRpcContract { Empty read(Empty request); }
}
