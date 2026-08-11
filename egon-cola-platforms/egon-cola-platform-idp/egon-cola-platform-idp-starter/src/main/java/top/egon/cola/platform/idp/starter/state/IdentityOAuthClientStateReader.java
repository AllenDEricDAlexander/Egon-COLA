package top.egon.cola.platform.idp.starter.state;

import top.egon.cola.platform.idp.core.oauth.OAuthClient;

import java.util.Objects;
import java.util.Optional;

/**
 * 定义读取 OAuth Client 当前运行态投影的端口。
 *
 * <p>Defines the port for reading current OAuth Client runtime projections.</p>
 */
@FunctionalInterface
public interface IdentityOAuthClientStateReader {

    /**
     * 按 Client 标识读取当前状态。
     *
     * <p>Reads current state by Client identifier.</p>
     *
     * @param clientId OAuth Client 标识；OAuth Client identifier
     * @return Client 状态；不存在时为空；Client state, or empty when absent
     */
    Optional<IdentityOAuthClientState> read(String clientId);

    /**
     * OAuth Client 运行态投影的可信只读视图。
     *
     * <p>Trusted read-only view of an OAuth Client runtime projection.</p>
     *
     * @param clientId OAuth Client 标识；OAuth Client identifier
     * @param clientType Client 类型；Client type
     * @param status Client 状态；Client status
     * @param boundSourceResourceServerId 绑定的源 Resource Server；bound source Resource Server
     * @param version Client 版本；Client version
     */
    record IdentityOAuthClientState(
            String clientId,
            OAuthClient.ClientType clientType,
            OAuthClient.Status status,
            String boundSourceResourceServerId,
            long version
    ) {

        /**
         * 校验并规范化 Client 投影。
         *
         * <p>Validates and normalizes the Client projection.</p>
         */
        public IdentityOAuthClientState {
            clientId = required(clientId, "clientId");
            clientType = Objects.requireNonNull(
                    clientType,
                    "clientType"
            );
            status = Objects.requireNonNull(status, "status");
            boundSourceResourceServerId = required(
                    boundSourceResourceServerId,
                    "boundSourceResourceServerId"
            );
            if (version < 0L) {
                throw new IllegalArgumentException(
                        "version must not be negative"
                );
            }
        }

        /** 校验必填文本；Validates required text. */
        private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }
    }
}
