package top.egon.cola.archetype.source.lightopen.infrastructure.user.client.impl;

import top.egon.cola.archetype.source.lightopen.domain.user.vos.ExternalUser;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.client.UserQueryClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class LocalUserQueryClientImpl implements UserQueryClient {
    @Override
    public Optional<ExternalUser> findExternalUser(String externalId) {
        if (!"ext-1".equals(externalId)) {
            return Optional.empty();
        }
        return Optional.of(new ExternalUser("ext-1", "Local User"));
    }
}
