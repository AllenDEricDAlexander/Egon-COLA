package top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.dao;

import top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.po.CoursePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface CourseDAO extends EgonColaMapper<CoursePO> {
    CoursePO selectByCode(@Param("code") String code);
}
