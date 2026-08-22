package top.egon.cola.platform.idp.admin.oauth.domain.vo;

import java.time.Instant;
import java.util.List;

/**
 * OAuth 客户端注册信息视图。
 *
 * <p>View of an OAuth client registration.</p>
 */
public record OAuthClientVO(
        String clientId,
        String clientName,
        String clientType,
        String status,
        boolean pkceRequired,
        int accessTokenTtlSeconds,
        int refreshTokenTtlSeconds,
        List<String> redirectUris,
        List<String> resourceUris,
        long version,
        Instant createdAt,
        Instant updatedAt,
        String appId,
        String secretHint,
        String secretStatus
) {

    /**
     * Creates the legacy safe view without Secret metadata.
     *
     * <p>Creates the legacy safe view without app or credential metadata.</p>
     */
    public OAuthClientVO(
            String clientId,
            String clientName,
            String clientType,
            String status,
            boolean pkceRequired,
            int accessTokenTtlSeconds,
            int refreshTokenTtlSeconds,
            List<String> redirectUris,
            List<String> resourceUris,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                clientId,
                clientName,
                clientType,
                status,
                pkceRequired,
                accessTokenTtlSeconds,
                refreshTokenTtlSeconds,
                redirectUris,
                resourceUris,
                version,
                createdAt,
                updatedAt,
                null,
                null,
                null
        );
    }
}
