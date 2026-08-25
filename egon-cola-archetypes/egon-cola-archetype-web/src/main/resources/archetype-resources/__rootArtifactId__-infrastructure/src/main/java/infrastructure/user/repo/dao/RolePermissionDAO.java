package ${package}.infrastructure.user.repo.dao;

import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RolePermissionDAO extends EgonColaMapper<RolePermissionPO> {
    List<RolePermissionPO> selectByRoleId(@Param("roleId") Long roleId);

    List<RolePermissionPO> selectByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    long countByRoleIdAndPermissionId(
            @Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
