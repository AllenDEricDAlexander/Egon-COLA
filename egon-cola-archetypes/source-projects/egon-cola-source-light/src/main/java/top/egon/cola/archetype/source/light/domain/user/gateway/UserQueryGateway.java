package top.egon.cola.archetype.source.light.domain.user.gateway;

import top.egon.cola.archetype.source.light.domain.user.vos.ExternalUser;

import java.util.Optional;

/** Outbound query gateway owned by the user domain. */
public interface UserQueryGateway {
    Optional<ExternalUser> findExternalUser(String externalId);
}
