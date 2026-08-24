package ${package}.infrastructure.user.repo.mapper;

import ${package}.infrastructure.user.repo.po.UserPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<UserPO> {

    long countByEmail(@Param("email") String email);
}
