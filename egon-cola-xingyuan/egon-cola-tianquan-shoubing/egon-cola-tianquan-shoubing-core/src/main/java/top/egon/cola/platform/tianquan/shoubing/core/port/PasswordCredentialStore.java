package top.egon.cola.platform.tianquan.shoubing.core.port;

import top.egon.cola.platform.tianquan.shoubing.core.identity.PasswordCredential;

import java.util.Optional;

public interface PasswordCredentialStore {

    Optional<PasswordCredential> findActive(String identitySub);

    PasswordCredential save(
            PasswordCredential credential,
            long expectedVersion
    );
}
