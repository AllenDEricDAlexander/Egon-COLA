package top.egon.cola.platform.tianquan.shoubing.admin.token.repo;

import jakarta.validation.Validator;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.OAuthSubjectRevokeCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService;
import top.egon.cola.platform.tianquan.shoubing.core.port.RefreshTokenStore;
import top.egon.cola.platform.tianquan.shoubing.core.token.RefreshTokenRecord;

/** Bridges the remaining core password-change port to durable OAuth subject revocation. */
@RequiredArgsConstructor
@Slf4j
@Validated
public class SubjectRevocationRefreshTokenStoreAdapter implements RefreshTokenStore {

    @Qualifier("tokenRevocationService")
    private final TokenRevocationService revocations;
    @Qualifier("defaultValidator")
    private final Validator validator;

    @Override
    public void revokeSubject(String identitySub, String reason, Instant now) {
        Objects.requireNonNull(now, "now");
        OAuthSubjectRevokeCommand command = new ValidationUtils(validator)
                .validate(new OAuthSubjectRevokeCommand(identitySub, reason));
        revocations.revokeSubject(command);
    }

    @Override
    public void create(RefreshTokenRecord record) {
        throw new UnsupportedOperationException("LEGACY_REFRESH_TOKEN_LIFECYCLE_RETIRED");
    }

    @Override
    public Optional<RefreshTokenRecord> findValid(String tokenDigest, Instant now) {
        throw new UnsupportedOperationException("LEGACY_REFRESH_TOKEN_LIFECYCLE_RETIRED");
    }

    @Override
    public void revokeToken(String tokenDigest, String reason, Instant now) {
        throw new UnsupportedOperationException("LEGACY_REFRESH_TOKEN_LIFECYCLE_RETIRED");
    }

    @Override
    public void expire(Instant now) {
        throw new UnsupportedOperationException("LEGACY_REFRESH_TOKEN_LIFECYCLE_RETIRED");
    }
}
