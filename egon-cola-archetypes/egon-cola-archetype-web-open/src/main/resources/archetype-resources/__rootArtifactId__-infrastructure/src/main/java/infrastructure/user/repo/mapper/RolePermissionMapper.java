package ${package}.infrastructure.user.repo.mapper;

import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface RolePermissionMapper extends BaseMapper<RolePermissionPO> {

    List<RolePermissionPO> selectByRoleId(@Param("roleId") Long roleId);

    List<RolePermissionPO> selectByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    long countByRoleIdAndPermissionId(
            @Param("roleId") Long roleId,
            @Param("permissionId") Long permissionId);
}
