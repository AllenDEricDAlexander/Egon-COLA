package ${package}.infrastructure.user.repo.mapper;

import ${package}.infrastructure.user.repo.po.UserRolePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserRoleMapper extends BaseMapper<UserRolePO> {
    List<UserRolePO> findByUserId(@Param("userId") Long userId);

    int insertRelation(UserRolePO relation);
}
