package top.egon.cola.platform.tianquan.shoubing.core.port;

import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;

import java.util.Optional;

public interface OAuthClientStore {

    Optional<OAuthClient> findById(String clientId);
}
