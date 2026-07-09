package ${package}.domain.user.service;

import ${package}.domain.user.vos.ExternalUser;

import java.util.Optional;

public interface UserQueryService {
    Optional<ExternalUser> findExternalUser(String externalId);
}
