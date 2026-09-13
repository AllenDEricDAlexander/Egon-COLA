package top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.dao;

import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.po.RolePermissionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.Collection;
import java.util.List;

/** MyBatis mapper for role-permission links. */
@Mapper
/** Shared active reads and versioned deletion are bound by this DAO XML. */
public interface RolePermissionDAO extends EgonColaMapper<RolePermissionPO> {
    List<RolePermissionPO> selectByRoleCodeIn(@Param("roleCodes") Collection<String> roleCodes);
}
