package top.egon.cola.component.gateway.admin.application.credential;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GatewayCredentialService {

    private final GatewayApplicationRepository applications;

    private final GatewayCredentialStore credentials;

    private final GatewayAuditLogRepository audits;

    private final GatewaySecretProtector protector;

    private final SecureRandom random;

    private final Clock clock;

    public GatewayCredentialService(
            GatewayApplicationRepository applications,
            GatewayCredentialStore credentials,
            GatewayAuditLogRepository audits,
            ObjectProvider<GatewaySecretProtector> protector) {
        this(
                applications,
                credentials,
                audits,
                protector.getIfAvailable(),
                new SecureRandom(),
                Clock.systemUTC()
        );
    }

    GatewayCredentialService(
            GatewayApplicationRepository applications,
            GatewayCredentialStore credentials,
            GatewayAuditLogRepository audits,
            GatewaySecretProtector protector,
            SecureRandom random,
            Clock clock) {
        this.applications = applications;
        this.credentials = credentials;
        this.audits = audits;
        this.protector = protector;
        this.random = random;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CredentialView> list(String applicationId) {
        requireApplication(applicationId);
        return credentials.list(applicationId).stream()
                .map(credential -> new CredentialView(
                        credential.id(),
                        credential.accessKey(),
                        credential.status(),
                        credential.validFrom(),
                        credential.validUntil()
                ))
                .toList();
    }

    @Transactional
    public IssuedCredential create(
            String applicationId,
            AdminActor actor,
            RequestAuditContext request) {
        requireApplication(applicationId);
        GatewaySecretProtector configured = requireProtector();
        String id = UuidV7.simpleString();
        String accessKey = "gw_" + token(18);
        String secret = token(32);
        Instant now = clock.instant();
        GatewaySecretProtector.ProtectedSecret protectedSecret =
                configured.protect(secret, aad(applicationId, accessKey));
        credentials.insert(new GatewayCredentialStore.CredentialRecord(
                id,
                applicationId,
                accessKey,
                protectedSecret.ciphertext(),
                protectedSecret.keyVersion(),
                "ACTIVE",
                now,
                null,
                now,
                now
        ));
        audit(actor, request, applicationId, accessKey, "CREATE");
        return new IssuedCredential(
                id,
                accessKey,
                secret,
                "ACTIVE",
                now,
                null
        );
    }

    @Transactional
    public IssuedCredential rotate(
            String applicationId,
            String keyId,
            Duration overlap,
            AdminActor actor,
            RequestAuditContext request) {
        if (overlap.isNegative() || overlap.compareTo(
                Duration.ofHours(24)
        ) > 0) {
            throw new IllegalArgumentException(
                    "credential overlap must be between 0 and 24 hours"
            );
        }
        GatewayCredentialStore.CredentialRecord current = required(
                applicationId,
                keyId
        );
        if ("REVOKED".equals(current.status())) {
            throw new IllegalArgumentException(
                    "revoked credential cannot be rotated"
            );
        }
        Instant now = clock.instant();
        credentials.overlap(
                current.id(),
                now.plus(overlap),
                now
        );
        IssuedCredential replacement = create(
                applicationId,
                actor,
                request
        );
        audit(actor, request, applicationId, current.accessKey(), "ROTATE");
        return replacement;
    }

    @Transactional
    public CredentialView revoke(
            String applicationId,
            String keyId,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayCredentialStore.CredentialRecord credential = required(
                applicationId,
                keyId
        );
        Instant now = clock.instant();
        credentials.revoke(credential.id(), now);
        audit(actor, request, applicationId, credential.accessKey(), "REVOKE");
        return new CredentialView(
                credential.id(),
                credential.accessKey(),
                "REVOKED",
                credential.validFrom(),
                now
        );
    }

    private GatewayCredentialStore.CredentialRecord required(
            String applicationId,
            String keyId) {
        return credentials.find(applicationId, keyId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway credential " + keyId + " was not found"
                ));
    }

    private void requireApplication(String applicationId) {
        if (applications.findByIdAndDeletedFalse(applicationId).isEmpty()) {
            throw new GatewayAdminNotFoundException(
                    "gateway application "
                            + applicationId
                            + " was not found"
            );
        }
    }

    private GatewaySecretProtector requireProtector() {
        if (protector == null) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_SECRET_PROTECTOR_NOT_CONFIGURED"
            );
        }
        return protector;
    }

    private String token(int bytes) {
        byte[] value = new byte[bytes];
        random.nextBytes(value);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }

    private String aad(String applicationId, String accessKey) {
        return applicationId + ":" + accessKey;
    }

    private void audit(
            AdminActor actor,
            RequestAuditContext request,
            String applicationId,
            String accessKey,
            String action) {
        audits.save(new GatewayAuditLogEntity(
                UuidV7.simpleString(),
                actor.actorId(),
                actor.actorType().name(),
                "MANAGEMENT_API",
                request.requestId(),
                request.traceId(),
                "GATEWAY_CREDENTIAL",
                accessKey,
                action,
                null,
                Map.of("applicationId", applicationId),
                null,
                null,
                true,
                null,
                clock.instant()
        ));
    }

    public record IssuedCredential(
            String id,
            String accessKey,
            String secret,
            String status,
            Instant validFrom,
            Instant validUntil
    ) {
    }

    public record CredentialView(
            String id,
            String accessKey,
            String status,
            Instant validFrom,
            Instant validUntil
    ) {
    }
}
