package ${package}.infrastructure.teaching.repo.mapper;

import ${package}.infrastructure.teaching.repo.po.ClassCourseSchedulePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ClassCourseScheduleMapper extends BaseMapper<ClassCourseSchedulePO> {
    List<ClassCourseSchedulePO> findBySchoolClassIdOrderByStartsAt(
            @Param("schoolClassId") Long schoolClassId);

    int insertSchedule(ClassCourseSchedulePO schedule);
}
