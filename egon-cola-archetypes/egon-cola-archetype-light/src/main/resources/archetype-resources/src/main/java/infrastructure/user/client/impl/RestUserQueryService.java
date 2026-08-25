package ${package}.infrastructure.user.client.impl;

import ${package}.domain.user.gateway.UserQueryGateway;
import ${package}.domain.user.vos.ExternalUser;
import ${package}.infrastructure.user.validators.UserInfrastructureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component("userQueryGateway")
@ConditionalOnProperty(name = "app.integrations.external-http.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RestUserQueryService implements UserQueryGateway {
    @Qualifier("userRestClient")
    private final RestClient restClient;
    @Qualifier("userInfrastructureValidator")
    private final UserInfrastructureValidator validator;

    @Override
    public Optional<ExternalUser> findExternalUser(String externalId) {
        try {
            ExternalUser user = restClient.get()
                    .uri("/users/{externalId}", externalId)
                    .retrieve()
                    .body(ExternalUser.class);
            validator.validateExternalUser(user, externalId);
            return Optional.of(user);
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        }
    }
}
