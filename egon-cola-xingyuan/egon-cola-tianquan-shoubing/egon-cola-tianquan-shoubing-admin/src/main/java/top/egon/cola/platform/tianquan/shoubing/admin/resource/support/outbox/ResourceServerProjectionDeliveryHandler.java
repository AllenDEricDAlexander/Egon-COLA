package top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthResourceRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerProjectionService;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;

/** Delivers post-commit Resource and service-Grant facts to Redis from current MP rows. */
@Slf4j
@RequiredArgsConstructor
public final class ResourceServerProjectionDeliveryHandler implements DeliveryHandler {

    private static final int SCHEMA_VERSION = 1;
    private static final Pattern TENANT_REALM = Pattern.compile("tenant:([1-9][0-9]{0,18})");

    @NonNull
    @Qualifier("oAuthResourceRepository")
    private final OAuthResourceRepository resources;
    @NonNull
    @Qualifier("oAuthClientRepository")
    private final OAuthClientRepository clients;
    @NonNull
    @Qualifier("oAuthGrantRepository")
    private final OAuthGrantRepository grants;
    @NonNull
    @Qualifier("oauthManagementRealmResolver")
    private final OAuthManagementRealmResolver realms;
    @NonNull
    @Qualifier("resourceServerProjectionService")
    private final ResourceServerProjectionService projections;
    @NonNull
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Override
    public String channel() {
        return TransactionalOutboxResourceServerEventAdapter.PROJECTION_CHANNEL;
    }

    @Override
    public void validateDestination(String destination) {
        if (!TransactionalOutboxResourceServerEventAdapter.RESOURCE_PROJECTION_DESTINATION.equals(destination)
                && !TransactionalOutboxResourceServerEventAdapter.SERVICE_GRANT_PROJECTION_DESTINATION
                        .equals(destination)) {
            throw new IllegalArgumentException("RESOURCE_PROJECTION_DESTINATION_UNSUPPORTED");
        }
    }

    @Override
    public DeliveryResult deliver(DeliveryContext context) {
        try {
            Objects.requireNonNull(context, "context");
            if (!channel().equals(context.channel())) {
                throw new IllegalArgumentException("RESOURCE_PROJECTION_CHANNEL_INVALID");
            }
            validateDestination(context.destination());
            JsonNode envelope = envelope(context);
            if (TransactionalOutboxResourceServerEventAdapter.RESOURCE_PROJECTION_DESTINATION
                    .equals(context.destination())) {
                projectResource(envelope);
            } else {
                projectServiceGrant(envelope);
            }
            return DeliveryResult.success();
        } catch (IllegalArgumentException invalid) {
            log.warn("Resource projection event rejected; messageId={}, destination={}, code={}",
                    context == null ? null : context.messageId(),
                    context == null ? null : context.destination(), safeMessage(invalid));
            return DeliveryResult.permanentFailure("RESOURCE_PROJECTION_EVENT_INVALID", safeMessage(invalid));
        } catch (RuntimeException unavailable) {
            log.warn("Resource projection delivery will retry; messageId={}, destination={}, causeType={}",
                    context == null ? null : context.messageId(),
                    context == null ? null : context.destination(), unavailable.getClass().getSimpleName());
            return DeliveryResult.retryableFailure("RESOURCE_PROJECTION_DEPENDENCY_UNAVAILABLE",
                    "Resource runtime projection dependency is unavailable");
        }
    }

    private JsonNode envelope(DeliveryContext context) {
        if (!"application/json".equals(context.contentType()) || !"1".equals(context.schemaVersion())) {
            throw new IllegalArgumentException("RESOURCE_PROJECTION_SCHEMA_VERSION_UNSUPPORTED");
        }
        try {
            JsonNode root = objectMapper.readTree(context.payload());
            if (root == null || !root.isObject()
                    || root.path("schemaVersion").asInt(-1) != SCHEMA_VERSION
                    || !context.destination().equals(requiredText(root, "eventType"))) {
                throw new IllegalArgumentException("RESOURCE_PROJECTION_EVENT_ENVELOPE_INVALID");
            }
            return root;
        } catch (IllegalArgumentException invalid) {
            throw invalid;
        } catch (Exception invalid) {
            throw new IllegalArgumentException("RESOURCE_PROJECTION_EVENT_ENVELOPE_INVALID", invalid);
        }
    }

    private void projectResource(JsonNode envelope) {
        JsonNode payload = envelope.path("payload");
        String resourceId = requiredText(payload, "resourceServerId");
        long resourceVersion = requiredVersion(payload, "resourceVersion");
        requireAggregate(envelope, "IDENTITY_RESOURCE_SERVER", resourceId, resourceVersion);
        TrustedRealmBO platform = realms.platformRealm();
        OAuthResourceBO current = resources.findByResourceServerId(platform, resourceId)
                .orElseThrow(() -> new IllegalArgumentException("RESOURCE_PROJECTION_RESOURCE_NOT_FOUND"));
        if (current.getVersion() < resourceVersion) {
            throw new IllegalStateException("RESOURCE_PROJECTION_RESOURCE_VERSION_NOT_VISIBLE");
        }
        OAuthClientBO managementClient = clients.findByClientId(platform, current.getManagementClientId())
                .orElseThrow(() -> new IllegalArgumentException("RESOURCE_PROJECTION_MANAGEMENT_CLIENT_NOT_FOUND"));
        projections.projectResource(current, managementClient);
    }

    private void projectServiceGrant(JsonNode envelope) {
        JsonNode payload = envelope.path("payload");
        String realmKey = requiredText(payload, "realmKey");
        TrustedRealmBO clientRealm = resolveRealm(realmKey, payload.get("tenantId"));
        String clientId = requiredText(payload, "clientId");
        String resourceId = requiredText(payload, "resourceServerId");
        if (!ResourceGrantType.CLIENT_CREDENTIALS.name().equals(requiredText(payload, "grantType"))) {
            throw new IllegalArgumentException("RESOURCE_PROJECTION_GRANT_TYPE_INVALID");
        }
        long grantVersion = requiredVersion(payload, "grantVersion");
        String aggregateId = realmKey + ":" + clientId + ":" + resourceId + ":CLIENT_CREDENTIALS";
        requireAggregate(envelope, "IDENTITY_SERVICE_RESOURCE_GRANT", aggregateId, grantVersion);

        OAuthClientBO client = clients.findByClientId(clientRealm, clientId)
                .orElseThrow(() -> new IllegalArgumentException("RESOURCE_PROJECTION_CLIENT_NOT_FOUND"));
        TrustedRealmBO platform = realms.platformRealm();
        OAuthResourceBO resource = resources.findByResourceServerId(platform, resourceId)
                .orElseThrow(() -> new IllegalArgumentException("RESOURCE_PROJECTION_RESOURCE_NOT_FOUND"));
        OAuthGrantBO current = grants.findGrant(clientRealm, client.getId(), resourceId,
                        ResourceGrantType.CLIENT_CREDENTIALS)
                .orElseGet(() -> tombstone(client, resource, grantVersion));
        if (current.getVersion() < grantVersion) {
            throw new IllegalStateException("RESOURCE_PROJECTION_GRANT_VERSION_NOT_VISIBLE");
        }
        if (current.getStatus() == ClientResourceGrant.Status.ACTIVE) {
            projections.projectServiceGrant(clientRealm, client, resource, current);
        } else {
            projections.deleteServiceGrant(clientRealm, client, resource, current);
        }
    }

    private TrustedRealmBO resolveRealm(String realmKey, JsonNode tenantIdNode) {
        if ("platform".equals(realmKey)) {
            if (tenantIdNode != null && !tenantIdNode.isNull()) {
                throw new IllegalArgumentException("RESOURCE_PROJECTION_PLATFORM_TENANT_MISMATCH");
            }
            return realms.platformRealm();
        }
        var matcher = TENANT_REALM.matcher(realmKey);
        if (!matcher.matches() || tenantIdNode == null || !tenantIdNode.isTextual()
                || !matcher.group(1).equals(tenantIdNode.textValue())) {
            throw new IllegalArgumentException("RESOURCE_PROJECTION_TENANT_REALM_INVALID");
        }
        TrustedRealmBO realm = realms.resolveTenantRealm(matcher.group(1));
        if (realm.getRealmType() != RealmTypeEnum.TENANT || !realmKey.equals(realm.getRealmKey())) {
            throw new IllegalArgumentException("RESOURCE_PROJECTION_REALM_REGISTRY_MISMATCH");
        }
        return realm;
    }

    private static OAuthGrantBO tombstone(OAuthClientBO client, OAuthResourceBO resource, long version) {
        return OAuthGrantBO.builder()
                .version(version)
                .clientPk(client.getId())
                .resourceServerId(resource.getResourceServerId())
                .grantType(ResourceGrantType.CLIENT_CREDENTIALS)
                .allowedScopes(List.of())
                .status(ClientResourceGrant.Status.DISABLED)
                .resourceVersion(resource.getVersion())
                .build();
    }

    private static void requireAggregate(JsonNode envelope, String aggregateType, String aggregateId, long version) {
        if (!aggregateType.equals(requiredText(envelope, "aggregateType"))
                || !aggregateId.equals(requiredText(envelope, "aggregateId"))
                || requiredVersion(envelope, "aggregateVersion") != version) {
            throw new IllegalArgumentException("RESOURCE_PROJECTION_AGGREGATE_MISMATCH");
        }
    }

    private static long requiredVersion(JsonNode source, String field) {
        JsonNode value = source.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0L) {
            throw new IllegalArgumentException("RESOURCE_PROJECTION_VERSION_INVALID");
        }
        return value.longValue();
    }

    private static String requiredText(JsonNode source, String field) {
        JsonNode value = source == null ? null : source.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()
                || !value.textValue().equals(value.textValue().trim())) {
            throw new IllegalArgumentException("RESOURCE_PROJECTION_FIELD_INVALID");
        }
        return value.textValue();
    }

    private static String safeMessage(IllegalArgumentException invalid) {
        return invalid.getMessage() == null ? "Invalid Resource projection event" : invalid.getMessage();
    }
}
