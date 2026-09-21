package top.egon.cola.archetype.source.lightopen.infrastructure.user.repo;

import top.egon.cola.archetype.source.lightopen.infrastructure.user.dao.UserRoleDAO;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.po.UserRolePO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import java.util.Collection;
import java.util.List;

/** Logical persistence boundary; query implementations remain explicit Mapper SQL. */
@Slf4j
@Validated
@Repository("userRoleRepository")
@RequiredArgsConstructor
public class UserRoleRepository extends EgonColaRepository<UserRoleDAO, UserRolePO> {
    @Getter
    @Qualifier("userRoleDAO")
    private final UserRoleDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public List<UserRolePO> selectByUserId(@NotNull @Positive Long userId) {
        return getBaseMapper().selectByUserId(userId);
    }
}
