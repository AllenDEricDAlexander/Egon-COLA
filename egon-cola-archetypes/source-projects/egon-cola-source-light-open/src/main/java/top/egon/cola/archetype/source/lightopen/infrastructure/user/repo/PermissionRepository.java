package top.egon.cola.archetype.source.lightopen.infrastructure.user.repo;

import top.egon.cola.archetype.source.lightopen.infrastructure.user.dao.PermissionDAO;
import top.egon.cola.archetype.source.lightopen.infrastructure.user.po.PermissionPO;
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
@Repository("permissionRepository")
@RequiredArgsConstructor
public class PermissionRepository extends EgonColaRepository<PermissionDAO, PermissionPO> {
    @Getter
    @Qualifier("permissionDAO")
    private final PermissionDAO baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;

    public List<PermissionPO> selectByCode(@NotBlank String code) {
        return getBaseMapper().selectByCode(code);
    }

    public List<PermissionPO> selectByCodeIn(@NotNull @Size(max = 10000) Collection<String> codes) {
        if (codes.isEmpty()) { return List.of(); }
        return getBaseMapper().selectByCodeIn(codes);
    }
}
