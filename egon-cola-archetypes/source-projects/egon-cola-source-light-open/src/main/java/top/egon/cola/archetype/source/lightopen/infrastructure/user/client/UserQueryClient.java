package top.egon.cola.archetype.source.lightopen.infrastructure.user.client;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.ExternalUser;

import java.util.Optional;

/** Transport-facing user query client; profiles bind a local or HTTP implementation. */
public interface UserQueryClient {
    Optional<ExternalUser> findExternalUser(@NotBlank String externalId);
}
