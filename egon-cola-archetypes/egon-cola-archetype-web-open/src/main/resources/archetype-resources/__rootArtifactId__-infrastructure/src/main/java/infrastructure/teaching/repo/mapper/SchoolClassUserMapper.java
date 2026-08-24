package ${package}.infrastructure.teaching.repo.mapper;

import ${package}.infrastructure.teaching.repo.po.SchoolClassUserPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface SchoolClassUserMapper extends BaseMapper<SchoolClassUserPO> {

    List<SchoolClassUserPO> selectByGradeIdAndSchoolClassId(
            @Param("gradeId") Long gradeId,
            @Param("schoolClassId") Long schoolClassId);

    long countByGradeIdAndSchoolClassIdAndUserId(
            @Param("gradeId") Long gradeId,
            @Param("schoolClassId") Long schoolClassId,
            @Param("userId") Long userId);
}
