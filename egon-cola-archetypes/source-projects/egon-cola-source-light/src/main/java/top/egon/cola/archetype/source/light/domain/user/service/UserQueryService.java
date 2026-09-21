package top.egon.cola.archetype.source.light.domain.user.service;

import jakarta.validation.constraints.NotBlank;
import top.egon.cola.archetype.source.light.domain.user.vos.ExternalUser;

import java.util.Optional;

/** Outbound user query capability owned by the domain. */
public interface UserQueryService {
    Optional<ExternalUser> findExternalUser(@NotBlank String externalId);
}
