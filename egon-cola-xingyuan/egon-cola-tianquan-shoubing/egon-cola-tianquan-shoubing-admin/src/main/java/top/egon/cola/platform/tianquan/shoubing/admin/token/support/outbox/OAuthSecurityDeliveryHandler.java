package top.egon.cola.platform.tianquan.shoubing.admin.token.support.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthRevocationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponent;
import top.egon.cola.platform.tianquan.shoubing.core.audit.OAuthSecurityChangedEvent;

@Component("oauthSecurityDeliveryHandler")
@Slf4j
@RequiredArgsConstructor
public final class OAuthSecurityDeliveryHandler implements DeliveryHandler {

    public static final String CHANNEL = OAuthSecurityChangedEvent.OUTBOX_CHANNEL;
    public static final String DESTINATION = OAuthSecurityChangedEvent.OUTBOX_DESTINATION;
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "eventId", "schemaVersion", "issuer", "realmKey", "revision", "targetType",
            "targetKey", "generation", "expiresAt", "reasonCode", "occurredAt", "traceId");

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("tenantIssuerService")
    private final TenantIssuerService tenantIssuerService;
    @Qualifier("oauthSecurityProjectionComponent")
    private final OAuthSecurityProjectionComponent projectionComponent;
    @Qualifier("oAuthRevocationRepository")
    private final OAuthRevocationRepository revocationRepository;

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public void validateDestination(String destination) {
        if (!DESTINATION.equals(destination)) {
            throw new IllegalArgumentException("OAUTH_SECURITY_OUTBOX_DESTINATION_UNSUPPORTED");
        }
    }

    @Override
    public DeliveryResult deliver(DeliveryContext context) {
        OAuthSecurityChangedEvent event = null;
        try {
            DeliveryContext delivery = Objects.requireNonNull(context, "context");
            validateDestination(delivery.destination());
            JsonNode payload = objectMapper.readTree(delivery.payload());
            validateFields(payload);
            event = objectMapper.treeToValue(payload, OAuthSecurityChangedEvent.class);
            if (!Objects.equals(delivery.messageId(), event.eventId())) {
                throw new IllegalArgumentException("OAUTH_SECURITY_EVENT_MESSAGE_ID_MISMATCH");
            }
            TrustedRealmBO trustedRealm = tenantIssuerService.resolveExactIssuer(event.issuer());
            if (!Objects.equals(trustedRealm.getRealmKey(), event.realmKey())) {
                throw new IllegalArgumentException("OAUTH_SECURITY_EVENT_REALM_MISMATCH");
            }
            projectionComponent.applyProjection(trustedRealm, event);
            markRevocationApplied(trustedRealm, event);
            return DeliveryResult.success();
        } catch (JsonProcessingException | IllegalArgumentException invalid) {
            return DeliveryResult.permanentFailure(
                    "OAUTH_SECURITY_EVENT_INVALID", "OAuth security event is invalid");
        } catch (OAuth2AuthenticationException rejected) {
            if ("temporarily_unavailable".equals(rejected.getError().getErrorCode())) {
                return DeliveryResult.retryableFailure(
                        "OAUTH_SECURITY_PROJECTION_UNAVAILABLE", "OAuth security projection is unavailable");
            }
            return DeliveryResult.permanentFailure(
                    "OAUTH_SECURITY_EVENT_TRUST_REJECTED", "OAuth security event trust was rejected");
        } catch (RuntimeException unavailable) {
            log.warn("OAuth security delivery failed; eventId={}", event == null ? "unparsed" : event.eventId());
            return DeliveryResult.retryableFailure(
                    "OAUTH_SECURITY_PROJECTION_UNAVAILABLE", "OAuth security projection is unavailable");
        }
    }

    private static void validateFields(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new IllegalArgumentException("OAUTH_SECURITY_EVENT_OBJECT_REQUIRED");
        }
        payload.fieldNames().forEachRemaining(name -> {
            if (!ALLOWED_FIELDS.contains(name)) {
                throw new IllegalArgumentException("OAUTH_SECURITY_EVENT_FIELD_UNSUPPORTED");
            }
        });
    }

    private void markRevocationApplied(TrustedRealmBO trustedRealm, OAuthSecurityChangedEvent event) {
        OAuthSecurityTargetEnum targetType = OAuthSecurityTargetEnum.fromCode(event.targetType());
        if (!targetType.isRevocationTarget()) {
            return;
        }
        OAuthRevocationBO current = revocationRepository.findByTarget(
                trustedRealm, targetType, event.targetKey()).orElse(null);
        if (current == null || "APPLIED".equals(current.getProjectionStatus())) {
            return;
        }
        OAuthRevocationBO applied = OAuthRevocationBO.builder()
                .id(current.getId())
                .version(Math.addExact(current.getVersion(), 1L))
                .targetType(current.getTargetType())
                .targetKey(current.getTargetKey())
                .reasonCode(current.getReasonCode())
                .revokedAt(current.getRevokedAt())
                .expiresAt(current.getExpiresAt())
                .revision(current.getRevision())
                .projectionStatus("APPLIED")
                .build();
        revocationRepository.save(trustedRealm, applied, current.getVersion());
    }
}
