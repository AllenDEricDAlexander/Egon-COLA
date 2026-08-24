package ${package}.infrastructure.teaching.repo.mapper;

import ${package}.infrastructure.teaching.repo.po.SchoolClassPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface SchoolClassMapper extends BaseMapper<SchoolClassPO> {

    SchoolClassPO selectByGradeIdAndId(
            @Param("gradeId") Long gradeId,
            @Param("id") Long id);

    long countByGradeIdAndNameIgnoreCase(
            @Param("gradeId") Long gradeId,
            @Param("name") String name);
}
