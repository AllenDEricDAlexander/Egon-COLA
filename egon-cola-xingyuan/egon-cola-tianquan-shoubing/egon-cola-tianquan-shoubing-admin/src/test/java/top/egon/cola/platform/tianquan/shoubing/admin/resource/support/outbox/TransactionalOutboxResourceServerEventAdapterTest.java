package top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionalOutboxResourceServerEventAdapterTest {

    @Test
    void enqueuesStableDisabledEnvelopeWithoutKeyMaterial() {
        TransactionalOutbox outbox = mock(TransactionalOutbox.class);
        when(outbox.enqueue(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new OutboxReceipt("event-1", "key-1", true));
        TransactionalOutboxResourceServerEventAdapter adapter =
                new TransactionalOutboxResourceServerEventAdapter(
                        outbox,
                        Clock.fixed(
                                Instant.parse("2026-08-10T00:00:00Z"),
                                ZoneOffset.UTC
                        )
                );
        OAuthResourceBO resource = resource();

        String eventId = adapter.enqueueDisabled(resource);

        assertThat(eventId).isNotBlank();
        ArgumentCaptor<OutboxMessage> captured =
                ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outbox).enqueue(captured.capture());
        OutboxMessage message = captured.getValue();
        assertThat(message.channel()).isEqualTo("identity-resource-runtime");
        assertThat(message.destination())
                .isEqualTo("identity.resource-server.disabled.v1");
        assertThat(message.idempotencyKey())
                .isEqualTo("permission-tianquan-shoubing-prod:disabled:1");
        assertThat(message.payload()).isInstanceOf(Map.class);
        assertThat(message.payload().toString())
                .contains("permission-tianquan-shoubing-prod", "permission", "tianquan-shoubing", "prod")
                .doesNotContain("jwk", "private", "secret", "credential");
    }

    @Test
    void enqueuesVersionedResourceProjectionIntentWithoutSnapshotFields() {
        TransactionalOutbox outbox = mock(TransactionalOutbox.class);
        when(outbox.enqueue(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new OutboxReceipt("event-2", "key-2", true));
        TransactionalOutboxResourceServerEventAdapter adapter = new TransactionalOutboxResourceServerEventAdapter(
                outbox, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        adapter.enqueueResourceProjection(resource());

        ArgumentCaptor<OutboxMessage> captured = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outbox).enqueue(captured.capture());
        OutboxMessage message = captured.getValue();
        assertThat(message.channel()).isEqualTo(TransactionalOutboxResourceServerEventAdapter.PROJECTION_CHANNEL);
        assertThat(message.destination())
                .isEqualTo(TransactionalOutboxResourceServerEventAdapter.RESOURCE_PROJECTION_DESTINATION);
        assertThat(message.idempotencyKey()).isEqualTo("permission-tianquan-shoubing-prod:projection:1");
        assertThat(message.payload().toString())
                .contains("permission-tianquan-shoubing-prod", "resourceVersion=1")
                .doesNotContain("resourceUri", "private", "secret", "credential");
    }

    @Test
    void enqueuesServiceGrantProjectionIntentBoundToRealmWithoutScopes() {
        TransactionalOutbox outbox = mock(TransactionalOutbox.class);
        when(outbox.enqueue(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new OutboxReceipt("event-3", "key-3", true));
        TransactionalOutboxResourceServerEventAdapter adapter = new TransactionalOutboxResourceServerEventAdapter(
                outbox, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        TrustedRealmBO realm = TrustedRealmBO.builder()
                .issuer(URI.create("https://idp.example.test/tenants/7"))
                .realmKey("tenant:7")
                .realmType(RealmTypeEnum.TENANT)
                .storagePartitionId(7L)
                .businessTenantId(7L)
                .trustVersion(1L)
                .build();
        OAuthClientBO client = OAuthClientBO.builder().id(11L).clientId("svc")
                .clientType(OAuthClient.ClientType.CONFIDENTIAL).purpose(OAuthClientPurposeEnum.SERVICE).build();
        OAuthGrantBO grant = OAuthGrantBO.builder().version(4L).clientPk(11L)
                .resourceServerId("resource").grantType(ResourceGrantType.CLIENT_CREDENTIALS)
                .allowedScopes(java.util.List.of("secret:read"))
                .status(ClientResourceGrant.Status.ACTIVE).resourceVersion(2L).build();

        adapter.enqueueServiceGrantProjection(realm, client, grant);

        ArgumentCaptor<OutboxMessage> captured = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outbox).enqueue(captured.capture());
        OutboxMessage message = captured.getValue();
        assertThat(message.destination())
                .isEqualTo(TransactionalOutboxResourceServerEventAdapter.SERVICE_GRANT_PROJECTION_DESTINATION);
        assertThat(message.idempotencyKey()).isEqualTo("tenant:7:svc:resource:CLIENT_CREDENTIALS:projection:4");
        assertThat(message.payload().toString())
                .contains("realmKey=tenant:7", "tenantId=7", "clientId=svc", "resourceServerId=resource")
                .doesNotContain("secret:read", "secretHash", "privateKey");
    }

    private static OAuthResourceBO resource() {
        return OAuthResourceBO.builder()
                .version(1L)
                .resourceServerId("permission-tianquan-shoubing-prod")
                .resourceUri(URI.create("https://api.egon.internal/prod/permission/tianquan-shoubing"))
                .bizCode("permission")
                .appCode("tianquan-shoubing")
                .environment("prod")
                .displayName("Tianquan-Shoubing Production")
                .managementClientId("tianquan-shoubing-service")
                .managementClientPk(1L)
                .rbacApplicationCode("tianquan-shoubing")
                .entryPermissionCode("tianquan-shoubing:access")
                .status(ResourceServerStatus.DISABLED)
                .admissionTicketTtl(Duration.ofMinutes(5))
                .build();
    }
}
