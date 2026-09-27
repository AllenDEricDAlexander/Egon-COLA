package top.egon.cola.component.rpc.tianshu.configdata;

import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerCallHandler;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.tianshu.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.rpc.tianshu.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.tianshu.client.DdcRpcClientHandle;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.DdcConfigRuntimeServiceGrpc;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.PullConfigRequest;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.PullConfigResponse;
import top.egon.cola.component.rpc.tianshu.security.DdcRpcMetadataKeys;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcAiResourceProperties;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcAiToggleDTO;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DdcTenantBootstrapTest {

    private static final String PREFIX = "egon.cola.component.tianshu";

    @Test
    void disabledBootstrapDoesNotNeedTenantOrRegisterRemoteFetcher() throws Exception {
        DefaultBootstrapContext bootstrap = new DefaultBootstrapContext();
        ConfigDataLocationResolverContext context = context(
                bootstrap,
                Map.of(PREFIX + ".enabled", false));

        assertThat(new DdcConfigDataLocationResolver().resolve(
                context,
                ConfigDataLocation.of("optional:tianshu:application.yml")))
                .isEmpty();
        assertThat(bootstrap.isRegistered(DdcConfigDataFetcher.class)).isFalse();
    }

    @Test
    void enabledOptionalBootstrapFailsClosedWhenTenantIsMissing() {
        Map<String, Object> properties = bootstrapProperties(false);
        DefaultBootstrapContext bootstrap = new DefaultBootstrapContext();

        assertThatThrownBy(() -> new DdcConfigDataLocationResolver().resolve(
                context(bootstrap, properties),
                ConfigDataLocation.of("optional:tianshu:application.yml")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenant-id");
        assertThat(bootstrap.isRegistered(DdcConfigDataFetcher.class)).isFalse();
    }

    @Test
    void earlyBinderAndResourceIdentityPreserveTheTenant() {
        DdcTenantProperties tenantProperties = bindTenant(421L);
        DdcConfigDataResource tenantA = resource(tenantProperties.getTenantId());
        DdcConfigDataResource tenantB = resource(422L);

        assertThat(tenantA.tenantId()).isEqualTo(421L);
        assertThat(tenantA).isNotEqualTo(tenantB);
        assertThat(tenantA.toString()).contains("421");
        assertThat(tenantA.toString()).doesNotContain("secret");
    }

    @Test
    void promptAndSkillTogglesRemainDisabledByDefault() {
        DdcAiResourceProperties properties = new DdcAiResourceProperties();
        DdcAiToggleDTO prompt = properties.getPrompt();
        DdcAiToggleDTO skill = properties.getSkill();

        assertThat(prompt.isEnabled()).isFalse();
        assertThat(skill.isEnabled()).isFalse();
    }

    @Test
    void bootstrapAndRuntimeClientsSignTheSameConfiguredTenant() throws Exception {
        List<String> signedTenants = new CopyOnWriteArrayList<>();
        Server server = NettyServerBuilder.forPort(0)
                .addService(ServerInterceptors.intercept(
                        new DdcConfigRuntimeServiceGrpc.DdcConfigRuntimeServiceImplBase() {
                            @Override
                            public void pullConfig(
                                    PullConfigRequest request,
                                    StreamObserver<PullConfigResponse> observer) {
                                observer.onNext(PullConfigResponse.getDefaultInstance());
                                observer.onCompleted();
                            }
                        },
                        new CapturingTenantInterceptor(signedTenants)))
                .build()
                .start();
        try {
            DdcRpcProperties rpcProperties = rpcProperties(server.getPort());
            DdcProperties ddcProperties = ddcProperties();
            DdcTenantProperties tenantProperties = bindTenant(421L);

            DdcConfigDataFetcher bootstrapFetcher = new DdcConfigDataFetcher(
                    ddcProperties, rpcProperties, tenantProperties);
            assertThat(bootstrapFetcher.load("application.yml")).isNull();

            DdcRpcClientFactory runtimeFactory = new DdcRpcClientFactory(
                    rpcProperties,
                    ddcProperties,
                    tenantProperties,
                    new RpcProcessIdentity("test", "test", "127.0.0.1", 1L, "instance-1"));
            try (DdcRpcClientHandle<top.egon.cola.component.tianshu.api.client.DdcConfigClient> handle =
                         runtimeFactory.configClient()) {
                assertThat(handle.client().pull()).isEmpty();
            }

            assertThat(signedTenants).containsExactly("421", "421");
        } finally {
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private ConfigDataLocationResolverContext context(
            DefaultBootstrapContext bootstrap,
            Map<String, Object> properties) {
        ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);
        when(context.getBinder()).thenReturn(new Binder(
                new MapConfigurationPropertySource(properties)));
        when(context.getBootstrapContext()).thenReturn(bootstrap);
        return context;
    }

    private Map<String, Object> bootstrapProperties(boolean tenantConfigured) {
        Map<String, Object> properties = new java.util.LinkedHashMap<>();
        properties.put(PREFIX + ".enabled", true);
        properties.put(PREFIX + ".biz-code", "orders");
        properties.put(PREFIX + ".env", "test");
        properties.put(PREFIX + ".namespace", "default");
        properties.put(PREFIX + ".app-code", "order-service");
        properties.put(PREFIX + ".rpc.target", "localhost:19080");
        properties.put(PREFIX + ".rpc.auth.enabled", false);
        if (tenantConfigured) {
            properties.put(PREFIX + ".tenant-id", 421L);
        }
        return Map.copyOf(properties);
    }

    private DdcTenantProperties bindTenant(long tenantId) {
        return new Binder(new MapConfigurationPropertySource(
                Map.of(PREFIX + ".tenant-id", tenantId)))
                .bind(PREFIX, Bindable.of(DdcTenantProperties.class))
                .orElseThrow(() -> new IllegalStateException("tenant properties did not bind"));
    }

    private DdcConfigDataResource resource(long tenantId) {
        return new DdcConfigDataResource(
                false, tenantId, "orders", "test", "default", "order-service", "application.yml");
    }

    private DdcProperties ddcProperties() {
        DdcProperties properties = new DdcProperties();
        properties.setEnabled(true);
        properties.setBizCode("orders");
        properties.setEnv("test");
        properties.setNamespace("default");
        properties.setAppCode("order-service");
        return properties;
    }

    private DdcRpcProperties rpcProperties(int port) {
        DdcRpcProperties properties = new DdcRpcProperties();
        properties.setTarget("localhost:" + port);
        properties.getAuth().setEnabled(true);
        properties.getAuth().getRuntime().setAccessKey("runtime-key");
        properties.getAuth().getRuntime().setSecretKey("runtime-secret");
        return properties;
    }

    private static final class CapturingTenantInterceptor implements ServerInterceptor {

        private final List<String> tenants;

        private CapturingTenantInterceptor(List<String> tenants) {
            this.tenants = tenants;
        }

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call,
                Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {
            tenants.add(headers.get(DdcRpcMetadataKeys.TENANT_ID));
            return next.startCall(call, headers);
        }
    }
}
