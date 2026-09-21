package top.egon.cola.archetype.source.light.infrastructure.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.light.domain.user.service.UserQueryService;
import top.egon.cola.archetype.source.light.domain.user.vos.ExternalUser;
import top.egon.cola.archetype.source.light.infrastructure.user.client.UserQueryClient;

import java.util.Optional;

/** User query capability; transport is delegated to the bound client implementation. */
@Validated
@Service("userQueryService")
@RequiredArgsConstructor
@Slf4j
public class UserQueryServiceImpl implements UserQueryService {
    @Qualifier("userQueryClient")
    private final UserQueryClient userQueryClient;

    @Override
    public Optional<ExternalUser> findExternalUser(String externalId) {
        return userQueryClient.findExternalUser(externalId);
    }
}
