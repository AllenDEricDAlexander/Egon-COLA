package ${package}.infrastructure.user.repo.mapper;

import ${package}.infrastructure.user.repo.po.UserRolePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserRoleMapper extends BaseMapper<UserRolePO> {

    List<UserRolePO> selectByUserId(@Param("userId") Long userId);

    List<UserRolePO> selectByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    long countByUserIdAndRoleId(
            @Param("userId") Long userId,
            @Param("roleId") Long roleId);
}
