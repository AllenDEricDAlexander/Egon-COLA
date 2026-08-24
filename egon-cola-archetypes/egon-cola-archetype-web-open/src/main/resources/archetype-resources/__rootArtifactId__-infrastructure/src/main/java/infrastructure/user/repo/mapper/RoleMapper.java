package ${package}.infrastructure.user.repo.mapper;

import ${package}.infrastructure.user.repo.po.RolePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface RoleMapper extends BaseMapper<RolePO> {

    RolePO selectByCode(@Param("code") String code);
}
