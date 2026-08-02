package top.egon.cola.platform.idp.core.port;

import top.egon.cola.platform.idp.core.identity.IdentityUser;

import java.util.Optional;

public interface IdentityUserStore {

    Optional<IdentityUser> findByNormalizedUsername(String normalizedUsername);

    Optional<IdentityUser> findById(String identitySub);

    IdentityUser save(IdentityUser user, long expectedVersion);
}
