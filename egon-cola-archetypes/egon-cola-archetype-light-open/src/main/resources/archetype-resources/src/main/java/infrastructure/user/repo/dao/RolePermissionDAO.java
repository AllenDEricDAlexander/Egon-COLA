package ${package}.infrastructure.user.repo.dao;

import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.Collection;
import java.util.List;

/** MyBatis mapper for role-permission links. */
@Mapper
public interface RolePermissionDAO extends EgonColaMapper<RolePermissionPO> {
    List<RolePermissionPO> selectByRoleCodeIn(@Param("roleCodes") Collection<String> roleCodes);
}
