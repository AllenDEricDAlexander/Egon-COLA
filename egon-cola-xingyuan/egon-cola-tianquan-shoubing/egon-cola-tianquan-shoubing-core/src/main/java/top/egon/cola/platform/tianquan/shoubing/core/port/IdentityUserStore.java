package top.egon.cola.platform.tianquan.shoubing.core.port;

import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUser;

import java.util.Optional;

public interface IdentityUserStore {

    Optional<IdentityUser> findByNormalizedUsername(String normalizedUsername);

    Optional<IdentityUser> findById(String identitySub);

    IdentityUser save(IdentityUser user, long expectedVersion);
}
