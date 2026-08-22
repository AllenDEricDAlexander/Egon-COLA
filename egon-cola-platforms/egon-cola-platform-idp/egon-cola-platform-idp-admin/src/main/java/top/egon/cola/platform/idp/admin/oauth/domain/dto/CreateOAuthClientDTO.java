package top.egon.cola.platform.idp.admin.oauth.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;

import java.util.List;

/**
 * 创建 OAuth Public 或机器 Confidential Client 的输入数据。
 *
 * <p>Input data for creating an OAuth Public or machine Confidential Client.</p>
 *
 * @param appId 稳定业务应用身份，CONFIDENTIAL 必填；stable application identity, required for
 *              CONFIDENTIAL clients
 * @param clientId Client 标识；Client identifier
 * @param clientName Client 展示名称；Client display name
 * @param clientType Client 类型，缺省为 PUBLIC；Client type, defaulting to PUBLIC
 * @param accessTokenTtlSeconds Access Token 有效秒数；access-token lifetime in seconds
 * @param refreshTokenTtlSeconds Refresh Token 配置秒数；refresh-token configuration in seconds
 * @param redirectUris Public Client 精确回调地址；exact redirect URIs for a Public Client
 * @param resourceUris Public Client USER_DELEGATION Resource URI；Public Client USER_DELEGATION
 * Resource URIs
 */
public record CreateOAuthClientDTO(
        @Pattern(regexp = "[a-z][a-z0-9-]{2,127}") String appId,
        @NotBlank String clientId,
        @NotBlank String clientName,
        IdentityClientEntity.ClientType clientType,
        @Positive int accessTokenTtlSeconds,
        @Positive int refreshTokenTtlSeconds,
        @NotNull List<@NotBlank String> redirectUris,
        @NotNull List<@NotBlank String> resourceUris
) {

    /**
     * 将旧请求中缺失的 Client 类型规范化为 PUBLIC。
     *
     * <p>Normalizes a missing Client type from legacy requests to PUBLIC.</p>
     */
    public CreateOAuthClientDTO {
        clientType = clientType == null
                ? IdentityClientEntity.ClientType.PUBLIC
                : clientType;
        if (appId != null && appId.isBlank()) {
            throw new IllegalArgumentException("appId is required when supplied");
        }
        if (clientType == IdentityClientEntity.ClientType.CONFIDENTIAL
                && (appId == null || appId.isBlank())) {
            throw new IllegalArgumentException(
                    "confidential client requires appId"
            );
        }
    }

    /**
     * 创建向后兼容的 PUBLIC Client 输入。
     *
     * <p>Creates a backward-compatible PUBLIC Client input.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param clientName Client 展示名称；Client display name
     * @param accessTokenTtlSeconds Access Token 有效秒数；access-token lifetime in seconds
     * @param refreshTokenTtlSeconds Refresh Token 有效秒数；refresh-token lifetime in seconds
     * @param redirectUris 精确回调地址；exact redirect URIs
     * @param resourceUris USER_DELEGATION Resource URI；USER_DELEGATION Resource URIs
     */
    public CreateOAuthClientDTO(
            String clientId,
            String clientName,
            int accessTokenTtlSeconds,
            int refreshTokenTtlSeconds,
            List<String> redirectUris,
            List<String> resourceUris
    ) {
        this(
                null,
                clientId,
                clientName,
                IdentityClientEntity.ClientType.PUBLIC,
                accessTokenTtlSeconds,
                refreshTokenTtlSeconds,
                redirectUris,
                resourceUris
        );
    }

    /**
     * 创建兼容旧调用方的 Client 输入。
     *
     * <p>Creates a compatibility input for callers that already provide the client type.</p>
     *
     * <p>Legacy Confidential bootstrap callers use the client id as appId until their
     * configuration is migrated to the explicit constructor.</p>
     */
    public CreateOAuthClientDTO(
            String clientId,
            String clientName,
            IdentityClientEntity.ClientType clientType,
            int accessTokenTtlSeconds,
            int refreshTokenTtlSeconds,
            List<String> redirectUris,
            List<String> resourceUris
    ) {
        this(
                clientType == IdentityClientEntity.ClientType.CONFIDENTIAL
                        ? clientId
                        : null,
                clientId,
                clientName,
                clientType,
                accessTokenTtlSeconds,
                refreshTokenTtlSeconds,
                redirectUris,
                resourceUris
        );
    }
}
