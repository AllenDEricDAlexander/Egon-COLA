package top.egon.cola.archetype.source.light.infrastructure.user.client.impl;

import top.egon.cola.archetype.source.light.domain.user.gateway.UserQueryGateway;
import top.egon.cola.archetype.source.light.domain.user.vos.ExternalUser;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class LocalUserQueryService implements UserQueryGateway {
    @Override
    public Optional<ExternalUser> findExternalUser(String externalId) {
        if (!"ext-1".equals(externalId)) {
            return Optional.empty();
        }
        return Optional.of(new ExternalUser("ext-1", "Local User"));
    }
}
