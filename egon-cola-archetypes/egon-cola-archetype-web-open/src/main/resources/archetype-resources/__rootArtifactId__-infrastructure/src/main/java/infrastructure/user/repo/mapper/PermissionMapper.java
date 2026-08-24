package ${package}.infrastructure.user.repo.mapper;

import ${package}.infrastructure.user.repo.po.PermissionPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PermissionMapper extends BaseMapper<PermissionPO> {

    PermissionPO selectByCode(@Param("code") String code);

    List<PermissionPO> selectPermissionsByIds(@Param("ids") Collection<Long> ids);
}
