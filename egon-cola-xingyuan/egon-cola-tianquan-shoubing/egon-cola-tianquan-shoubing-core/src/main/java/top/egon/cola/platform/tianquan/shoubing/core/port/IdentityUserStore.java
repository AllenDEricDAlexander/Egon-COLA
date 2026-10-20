package top.egon.cola.platform.tianquan.shoubing.core.port;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUser;

import java.util.Optional;

public interface IdentityUserStore {

    Optional<IdentityUser> findByNormalizedUsername(@NotBlank @Size(max = 128) String normalizedUsername);

    Optional<IdentityUser> findById(@NotBlank @Size(max = 64) String identitySub);

    IdentityUser save(@NotNull IdentityUser user, @PositiveOrZero long expectedVersion);
}
