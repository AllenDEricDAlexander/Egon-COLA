package top.egon.cola.platform.idp.core.port;

import top.egon.cola.platform.idp.core.oauth.OAuthClient;

import java.util.Optional;

public interface OAuthClientStore {

    Optional<OAuthClient> findById(String clientId);
}
