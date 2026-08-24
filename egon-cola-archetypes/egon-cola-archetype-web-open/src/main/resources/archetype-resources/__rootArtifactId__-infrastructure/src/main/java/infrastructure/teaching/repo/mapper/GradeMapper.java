package ${package}.infrastructure.teaching.repo.mapper;

import ${package}.infrastructure.teaching.repo.po.GradePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface GradeMapper extends BaseMapper<GradePO> {

    GradePO selectByCode(@Param("code") String code);

    long countByCode(@Param("code") String code);
}
