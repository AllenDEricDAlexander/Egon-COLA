package top.egon.cola.archetype.source.light.infrastructure.teaching.repo.dao;

import top.egon.cola.archetype.source.light.infrastructure.teaching.repo.po.CoursePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.List;

/** MyBatis mapper for courses. */
@Mapper
public interface CourseDAO extends EgonColaMapper<CoursePO> {
    List<CoursePO> selectByCourseCode(@Param("courseCode") String courseCode);
}
