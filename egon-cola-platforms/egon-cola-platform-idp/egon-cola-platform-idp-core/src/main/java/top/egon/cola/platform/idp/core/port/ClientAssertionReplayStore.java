package top.egon.cola.platform.idp.core.port;

import java.time.Instant;

/**
 * private_key_jwt Client Assertion 防重放端口。
 *
 * <p>Replay-prevention port for private_key_jwt Client Assertions.</p>
 */
@FunctionalInterface
public interface ClientAssertionReplayStore {

    /**
     * 原子记录 Client 和 jti；已存在时返回失败。
     *
     * <p>Atomically records a Client and jti, returning failure when already present.</p>
     *
     * @param clientId  Client 标识；Client identifier
     * @param tokenId   Assertion jti；Assertion jti
     * @param expiresAt 防重放记录到期时间；replay-record expiration
     * @return 首次记录时为 {@code true}；{@code true} when recorded for the first time
     */
    boolean markIfAbsent(String clientId, String tokenId, Instant expiresAt);
}
