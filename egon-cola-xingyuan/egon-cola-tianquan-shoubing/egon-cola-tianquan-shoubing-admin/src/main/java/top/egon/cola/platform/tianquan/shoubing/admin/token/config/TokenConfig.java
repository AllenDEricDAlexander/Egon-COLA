package top.egon.cola.platform.tianquan.shoubing.admin.token.config;

import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repo.SubjectRevocationRefreshTokenStoreAdapter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService;
import top.egon.cola.platform.tianquan.shoubing.core.port.RefreshTokenStore;

/** Keeps only the core password-change subject-revocation compatibility port. */
@Configuration(proxyBeanMethods = false)
public class TokenConfig {

    @Bean("refreshTokenStore")
    RefreshTokenStore refreshTokenStore(
            @Qualifier("tokenRevocationService") TokenRevocationService revocations,
            @Qualifier("defaultValidator") Validator validator) {
        return new SubjectRevocationRefreshTokenStoreAdapter(revocations, validator);
    }
}
