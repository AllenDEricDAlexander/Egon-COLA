package top.egon.cola.archetype.source.web.infrastructure.user.repo;

import top.egon.cola.archetype.source.web.infrastructure.user.repo.dao.RolePermissionDAO;
import top.egon.cola.archetype.source.web.infrastructure.user.repo.po.RolePermissionPO;
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
import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Max;

/** Logical persistence boundary; query implementations remain explicit Mapper SQL. */
@Slf4j
@Validated
@Repository("rolePermissionRepository")
@RequiredArgsConstructor
public class RolePermissionRepository extends EgonColaRepository<RolePermissionDAO, RolePermissionPO> {
    @Getter
    @Qualifier("rolePermissionDAO")
    private final RolePermissionDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public List<RolePermissionPO> selectByRoleId(@Positive Long roleId) {
        return getBaseMapper().selectByRoleId(roleId);
    }

    public List<RolePermissionPO> selectByRoleIds(@NotNull Collection<Long> roleIds) {
        return getBaseMapper().selectByRoleIds(roleIds);
    }

    public long countByRoleIdAndPermissionId(@Positive Long roleId, @Positive Long permissionId) {
        return getBaseMapper().countByRoleIdAndPermissionId(roleId, permissionId);
    }
}
