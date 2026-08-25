#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo.dao;

import ${package}.infrastructure.course.repo.po.CoursePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface CourseDAO extends EgonColaMapper<CoursePO> {
    CoursePO selectByCode(@Param("code") String code);
}
