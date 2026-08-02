package top.egon.cola.platform.idp.core.port;

import top.egon.cola.platform.idp.core.oauth.AuthorizationCode;

import java.time.Duration;

public interface AuthorizationCodeStore {

    void put(String codeDigest, AuthorizationCode code, Duration ttl);

    AuthorizationCode consume(String codeDigest);
}
