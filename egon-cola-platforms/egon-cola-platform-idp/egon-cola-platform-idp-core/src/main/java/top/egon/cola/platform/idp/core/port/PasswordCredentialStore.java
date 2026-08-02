package top.egon.cola.platform.idp.core.port;

import top.egon.cola.platform.idp.core.identity.PasswordCredential;

import java.util.Optional;

public interface PasswordCredentialStore {

    Optional<PasswordCredential> findActive(String identitySub);

    PasswordCredential save(
            PasswordCredential credential,
            long expectedVersion
    );
}
