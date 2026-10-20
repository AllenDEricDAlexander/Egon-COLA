package top.egon.cola.platform.tianquan.shoubing.core.port;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import top.egon.cola.platform.tianquan.shoubing.core.identity.PasswordCredential;

import java.util.Optional;

public interface PasswordCredentialStore {

    Optional<PasswordCredential> findActive(@NotBlank @Size(max = 64) String identitySub);

    PasswordCredential save(
            @NotNull PasswordCredential credential,
            @PositiveOrZero long expectedVersion
    );
}
