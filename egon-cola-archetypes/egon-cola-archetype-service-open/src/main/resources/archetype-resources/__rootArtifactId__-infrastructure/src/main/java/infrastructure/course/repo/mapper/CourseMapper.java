#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo.mapper;

import ${package}.infrastructure.course.repo.po.CoursePo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CourseMapper extends BaseMapper<CoursePo> {

    CoursePo selectByCode(@Param("code") String code);

    List<CoursePo> selectPage(
            @Param("offset") long offset,
            @Param("limit") int limit);

    long countAll();
}
