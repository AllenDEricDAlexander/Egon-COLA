package top.egon.cola.component.rpc.ddc.registry;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.model.lease.*;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;
import top.egon.cola.component.rpc.provider.registration.RpcLeaseOperationResult;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLease;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLeaseIdentity;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistration;
import top.egon.cola.platform.idp.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.idp.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.idp.starter.client.IdpServiceTokenRequest;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DdcRpcProviderRegistryTest {

    @Test
    void mapsNeutralProviderAndLeaseOperationsToDdc() {
        DdcServiceRegistryClient client = mock(DdcServiceRegistryClient.class);
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        when(client.register(any())).thenReturn(new DdcLeaseSession(
                "instance-1", "lease-1", DdcLeaseRole.RPC_PROVIDER,
                30, 10, now, now.plusSeconds(30)));
        when(client.heartbeat(any(DdcServiceLeaseRequest.class))).thenReturn(
                new DdcLeaseOperationResult(
                        DdcLeaseOperationStatus.RENEWED, now.plusSeconds(30)));
        when(client.deregister("instance-1", "lease-1")).thenReturn(
                new DdcLeaseOperationResult(DdcLeaseOperationStatus.DELETED, null));
        DdcRpcProviderRegistry registry = new DdcRpcProviderRegistry(
                client,
                "biz",
                "app",
                serviceClient(new AtomicInteger(), "service-token"),
                idpProperties());
        RpcProviderRegistration registration = new RpcProviderRegistration(
                new RpcServiceIdentity("OrderService", "default", "1.0.0"),
                new RpcProcessIdentity("orders", "test", "127.0.0.1", 1, "instance-1"),
                "127.0.0.1", 19090, false,
                Map.of("gateway.weight", "80"), 30, 10);

        RpcProviderLease lease = registry.register(registration);
        RpcProviderLeaseIdentity identity = new RpcProviderLeaseIdentity(
                registration.serviceIdentity(), lease.instanceId(), lease.leaseId());

        assertThat(registry.heartbeat(identity).renewed()).isTrue();
        assertThat(registry.deregister(identity).status())
                .isEqualTo(RpcLeaseOperationResult.Status.DELETED);
        verify(client).register(argThat((DdcServiceRegistration value) ->
                value.serviceKey().serviceKind() == DdcServiceKind.RPC_PROVIDER
                        && value.serviceKey().protocol().equals("grpc")
                        && value.instanceId().equals("instance-1")
                        && value.registrationToken().equals("service-token")));
        verify(client).heartbeat(argThat((DdcServiceLeaseRequest value) ->
                value.getServiceKey().serviceKind()
                        == DdcServiceKind.RPC_PROVIDER
                        && value.getRegistrationToken()
                        .equals("service-token")));
    }

    @Test
    void acquiresCurrentTicketAtEachProviderLeaseBoundary() {
        DdcServiceRegistryClient client = mock(DdcServiceRegistryClient.class);
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        when(client.register(any())).thenReturn(new DdcLeaseSession(
                "instance-1", "lease-1", DdcLeaseRole.RPC_PROVIDER,
                30, 10, now, now.plusSeconds(30)));
        when(client.heartbeat(any(DdcServiceLeaseRequest.class))).thenReturn(
                new DdcLeaseOperationResult(
                        DdcLeaseOperationStatus.RENEWED,
                        now.plusSeconds(30)
                ));
        AtomicInteger calls = new AtomicInteger();
        DdcRpcProviderRegistry registry = new DdcRpcProviderRegistry(
                client,
                "biz",
                "app",
                serviceClient(calls, "service-token-1", "service-token-2"),
                idpProperties()
        );
        RpcProviderRegistration registration = new RpcProviderRegistration(
                new RpcServiceIdentity(
                        "OrderService", "default", "1.0.0"
                ),
                new RpcProcessIdentity(
                        "orders", "test", "127.0.0.1", 1, "instance-1"
                ),
                "127.0.0.1", 19090, false,
                Map.of(), 30, 10
        );

        RpcProviderLease lease = registry.register(registration);
        registry.heartbeat(new RpcProviderLeaseIdentity(
                registration.serviceIdentity(),
                lease.instanceId(),
                lease.leaseId()
        ));

        verify(client).register(argThat(value -> value.registrationToken()
                .equals("service-token-1")));
        verify(client).heartbeat(argThat(value -> value.getRegistrationToken()
                .equals("service-token-2")));
        assertThat(calls).hasValue(2);
    }

    @Test
    void registersRpcProviderWithoutGatewayDefinitionMetadata() {
        DdcServiceRegistryClient client = mock(
                DdcServiceRegistryClient.class
        );
        Instant now = Instant.parse("2026-08-09T00:00:00Z");
        when(client.register(any())).thenReturn(new DdcLeaseSession(
                "internal-instance",
                "internal-lease",
                DdcLeaseRole.RPC_PROVIDER,
                45,
                15,
                now,
                now.plusSeconds(45)
        ));
        DdcRpcProviderRegistry registry = new DdcRpcProviderRegistry(
                client,
                "trade",
                "orders",
                serviceClient(new AtomicInteger(), "service-token"),
                idpProperties()
        );
        RpcProviderRegistration registration = new RpcProviderRegistration(
                new RpcServiceIdentity(
                        "InternalOrderService",
                        "internal",
                        "2.1.0"
                ),
                new RpcProcessIdentity(
                        "orders",
                        "prod",
                        "10.0.0.8",
                        4123,
                        "internal-instance"
                ),
                "10.0.0.8",
                19091,
                true,
                Map.of("region", "cn-east-1"),
                45,
                15
        );

        registry.register(registration);

        ArgumentCaptor<DdcServiceRegistration> captor =
                ArgumentCaptor.forClass(DdcServiceRegistration.class);
        verify(client).register(captor.capture());
        DdcServiceRegistration captured = captor.getValue();
        assertThat(captured.serviceKey().bizCode()).isEqualTo("trade");
        assertThat(captured.serviceKey().appCode()).isEqualTo("orders");
        assertThat(captured.serviceKey().env()).isEqualTo("prod");
        assertThat(captured.serviceKey().serviceKind())
                .isEqualTo(DdcServiceKind.RPC_PROVIDER);
        assertThat(captured.serviceKey().serviceName())
                .isEqualTo("InternalOrderService");
        assertThat(captured.serviceKey().group()).isEqualTo("internal");
        assertThat(captured.serviceKey().version()).isEqualTo("2.1.0");
        assertThat(captured.serviceKey().protocol()).isEqualTo("grpc");
        assertThat(captured.instanceId()).isEqualTo("internal-instance");
        assertThat(captured.host()).isEqualTo("10.0.0.8");
        assertThat(captured.port()).isEqualTo(19091);
        assertThat(captured.secure()).isTrue();
        assertThat(captured.metadata())
                .containsExactlyEntriesOf(Map.of("region", "cn-east-1"));
        assertThat(captured.leaseSeconds()).isEqualTo(45);
        assertThat(captured.heartbeatIntervalSeconds()).isEqualTo(15);
    }

    private IdpStarterProperties idpProperties() {
        IdpStarterProperties properties = new IdpStarterProperties();
        properties.setResourceUri(java.net.URI.create("https://api.example/ddc"));
        IdpStarterProperties.ServiceClient client =
                new IdpStarterProperties.ServiceClient();
        client.setAppId("ddc-app");
        client.setRegistrationId("ddc-registration");
        properties.setServiceClient(client);
        return properties;
    }

    private IdpServiceOAuth2Client serviceClient(
            AtomicInteger calls,
            String... tokens) {
        IdpServiceOAuth2Client client = mock(IdpServiceOAuth2Client.class);
        when(client.authorize(any(IdpServiceTokenRequest.class)))
                .thenAnswer(invocation -> {
                    int sequence = calls.getAndIncrement();
                    return accessToken(tokens[Math.min(sequence, tokens.length - 1)]);
                });
        return client;
    }

    private OAuth2AccessToken accessToken(String value) {
        Instant issuedAt = Instant.now();
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                value,
                issuedAt,
                issuedAt.plusSeconds(300)
        );
    }
}
