package top.egon.cola.archetype.source.light.infrastructure.user.repo;

import top.egon.cola.archetype.source.light.infrastructure.user.repo.dao.UserDAO;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.po.UserPO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.cache.EgonColaCachePort;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import java.util.Collection;
import java.util.List;

/** Logical persistence boundary; query implementations remain explicit Mapper SQL. */
@Slf4j
@Validated
@Repository("userRepository")
@RequiredArgsConstructor
public class UserRepository extends EgonColaRepository<UserDAO, UserPO> {
    @Getter
    @Qualifier("userDAO")
    private final UserDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaModelValidationUtils")
    private final EgonColaModelValidationUtils modelValidationUtils;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaMdcTenantIdProvider")
    private final EgonColaTenantIdProvider tenantIdProvider;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    // Field name is the mp-ext seam contract: lombok's getCachePortProvider() overrides EgonColaRepository's hook.
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egonColaCachePort")
    private final ObjectProvider<EgonColaCachePort> cachePortProvider;

    public List<UserPO> selectByExternalId(@NotBlank String externalId) {
        return getBaseMapper().selectByExternalId(externalId);
    }
}
