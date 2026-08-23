package ${package}.infrastructure.user.repo.mapper;

import ${package}.infrastructure.user.repo.po.PermissionPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface PermissionMapper extends BaseMapper<PermissionPO> {
    List<PermissionPO> findByCodesOrderByCode(@Param("codes") Collection<String> codes);
}
