package ${package}.infrastructure.teaching.repo.mapper;

import ${package}.infrastructure.teaching.repo.po.CoursePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface CourseMapper extends BaseMapper<CoursePO> {
    CoursePO findByCourseCode(@Param("courseCode") String courseCode);
}
