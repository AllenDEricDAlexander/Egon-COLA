package top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasRegisteredClientConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;

@Repository("tenantRegisteredClientRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class TenantRegisteredClientRepository implements RegisteredClientRepository {

    @Qualifier("tenantIssuerService")
    private final TenantIssuerService tenantIssuerService;
    @Qualifier("oAuthClientRepository")
    private final OAuthClientRepository clientRepository;
    @Qualifier("sasRegisteredClientConverterImpl")
    private final SasRegisteredClientConverter converter;

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new IllegalStateException("OAUTH_CLIENT_REGISTRATION_MUST_USE_ADMIN_SERVICE");
    }

    @Override
    public RegisteredClient findById(String id) {
        Long clientPk = parsePositiveId(id);
        if (clientPk == null) {
            return null;
        }
        TrustedRealmBO trustedRealm = tenantIssuerService.resolveCurrentIssuer();
        return clientRepository.findById(trustedRealm, clientPk)
                .filter(TenantRegisteredClientRepository::isActive)
                .map(converter::toTarget)
                .orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        if (clientId == null || clientId.isBlank() || !clientId.equals(clientId.trim())
                || clientId.length() > 128) {
            return null;
        }
        TrustedRealmBO trustedRealm = tenantIssuerService.resolveCurrentIssuer();
        return clientRepository.findByClientId(trustedRealm, clientId)
                .filter(TenantRegisteredClientRepository::isActive)
                .map(converter::toTarget)
                .orElse(null);
    }

    private static boolean isActive(OAuthClientBO client) {
        return Objects.requireNonNull(client, "client").getStatus() == OAuthClient.Status.ACTIVE;
    }

    private static Long parsePositiveId(String id) {
        if (id == null || !id.matches("[1-9][0-9]{0,18}")) {
            return null;
        }
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException failure) {
            return null;
        }
    }
}
