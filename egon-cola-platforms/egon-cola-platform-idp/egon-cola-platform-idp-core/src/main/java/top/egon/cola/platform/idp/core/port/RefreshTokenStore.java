package top.egon.cola.platform.idp.core.port;

import top.egon.cola.platform.idp.core.token.RefreshTokenRecord;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenStore {

    void create(RefreshTokenRecord record);

    Optional<RefreshTokenRecord> findValid(String tokenDigest, Instant now);

    void revokeToken(String tokenDigest, String reason, Instant now);

    void revokeSubject(String identitySub, String reason, Instant now);

    void expire(Instant now);

}
