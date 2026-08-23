package ${package}.infrastructure.user.repo.mapper;

import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface RolePermissionMapper extends BaseMapper<RolePermissionPO> {
    List<RolePermissionPO> findByRoleCode(@Param("roleCode") String roleCode);

    List<RolePermissionPO> findByRoleCodes(@Param("roleCodes") Collection<String> roleCodes);

    int insertRelation(RolePermissionPO relation);
}
