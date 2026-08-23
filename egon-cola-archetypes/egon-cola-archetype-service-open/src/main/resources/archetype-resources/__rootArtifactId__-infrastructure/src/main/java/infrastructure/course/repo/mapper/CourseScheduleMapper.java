#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo.mapper;

import ${package}.infrastructure.course.repo.po.CourseSchedulePo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CourseScheduleMapper extends BaseMapper<CourseSchedulePo> {

    CourseSchedulePo selectByCourseIdAndId(
            @Param("courseId") Long courseId,
            @Param("id") Long id);

    List<CourseSchedulePo> selectOverlapping(
            @Param("courseId") Long courseId,
            @Param("classId") Long classId,
            @Param("endsAt") Instant endsAt,
            @Param("startsAt") Instant startsAt);
}
