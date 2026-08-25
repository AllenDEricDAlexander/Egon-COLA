package ${package}.domain.user.gateway;

import ${package}.domain.user.vos.ExternalUser;

import java.util.Optional;

/** Outbound query gateway owned by the user domain. */
public interface UserQueryGateway {
    Optional<ExternalUser> findExternalUser(String externalId);
}
