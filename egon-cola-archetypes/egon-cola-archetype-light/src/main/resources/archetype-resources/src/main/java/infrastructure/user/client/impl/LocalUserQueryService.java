package ${package}.infrastructure.user.client.impl;

import ${package}.domain.user.service.UserQueryService;
import ${package}.domain.user.vos.ExternalUser;

import java.util.Optional;

public final class LocalUserQueryService implements UserQueryService {
    @Override
    public Optional<ExternalUser> findExternalUser(String externalId) {
        if (!"ext-1".equals(externalId)) {
            return Optional.empty();
        }
        return Optional.of(new ExternalUser("ext-1", "Local User"));
    }
}
