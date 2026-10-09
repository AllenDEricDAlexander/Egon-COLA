package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.ToString;

/** Typed source-page envelope. The nested payloads are read-only migration rows. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class IdentityMigrationRowDTO {

    private String sourceSnapshotId;
    private SourceType sourceType;
    private String sourceKey;
    private String sourceFingerprint;
    private Long targetId;
    private Long directoryPartitionId;
    private long sourceVersion;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private UserRowDTO user;
    private CredentialRowDTO credential;
    private TenantRowDTO tenant;
    private MembershipRowDTO membership;
    private AuditRowDTO audit;
    private ClientRowDTO client;
    private RedirectUriRowDTO redirectUri;
    private GrantRowDTO grant;
    private ResourceRowDTO resource;
    private OutboxEventMetadataRowDTO outboxEvent;

    public enum SourceType {
        USER,
        CREDENTIAL,
        TENANT,
        MEMBERSHIP,
        AUDIT
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class UserRowDTO {
        private String id;
        private String username;
        private String normalizedUsername;
        private String displayName;
        private String status;
        private Integer failedLoginCount;
        private Instant lockedUntil;
        private Instant lastLoginAt;
        private long version;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class CredentialRowDTO {
        private String id;
        private String identitySub;
        private String credentialType;
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        @JsonIgnore
        private String passwordHash;
        private Instant passwordChangedAt;
        private Boolean mustChangePassword;
        private String status;
        private long version;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class TenantRowDTO {
        private String id;
        private String tenantCode;
        private String tenantName;
        private String status;
        @JsonIgnore
        private String settings;
        private long version;
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;
        private String updatedBy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class MembershipRowDTO {
        private String id;
        private String tenantId;
        private String identitySub;
        private String status;
        private long version;
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;
        private String updatedBy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class AuditRowDTO {
        private String id;
        private String eventType;
        private String actorSub;
        private String targetSub;
        private String tenantId;
        private String sourceIp;
        private String userAgent;
        private String result;
        private String reason;
        private String traceId;
        private Instant occurredAt;
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        @JsonIgnore
        private String payload;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class ClientRowDTO {
        private String clientId;
        private String appId;
        private String clientName;
        private String clientType;
        private String status;
        private Boolean pkceRequired;
        private Integer accessTokenTtlSeconds;
        private Integer refreshTokenTtlSeconds;
        private long version;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class RedirectUriRowDTO {
        private String id;
        private String clientId;
        private String redirectUri;
        private Instant createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class GrantRowDTO {
        private String id;
        private String clientId;
        private String resourceServerId;
        private String grantType;
        private String grantContext;
        private String tenantId;
        private String allowedScopes;
        private String status;
        private long version;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class ResourceRowDTO {
        private String id;
        private String resourceServerId;
        private String resourceUri;
        private String bizCode;
        private String appCode;
        private String environment;
        private String displayName;
        private String managementClientId;
        private String rbacApplicationCode;
        private String entryPermissionCode;
        private String status;
        private long version;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class OutboxEventMetadataRowDTO {
        private String id;
        private String aggregateType;
        private String aggregateId;
        private String eventType;
        private String status;
        private Integer attemptCount;
        private Instant nextAttemptAt;
        private Instant createdAt;
        private Instant publishedAt;
    }

    /** Operator-supplied, snapshot-bound source-client ownership and scope decisions. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class ClientOwnershipMapDTO {
        private Integer schemaVersion;
        private String sourceSnapshotId;
        private List<ClientOwnershipAssignmentDTO> assignments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class ClientOwnershipAssignmentDTO {
        private String clientId;
        private String realmKey;
        private String purpose;
        private Map<String, List<String>> approvedGrantScopesByResource;
        private String scopeApprovalId;
    }
}
