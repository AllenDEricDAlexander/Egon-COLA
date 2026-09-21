package top.egon.cola.archetype.source.service.infrastructure.course.dao;

import top.egon.cola.archetype.source.service.infrastructure.course.po.CoursePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface CourseDAO extends EgonColaMapper<CoursePO> {
    CoursePO selectByCode(@Param("code") String code);
    com.baomidou.mybatisplus.core.metadata.IPage<CoursePO> selectActivePage(@Param("page") com.baomidou.mybatisplus.core.metadata.IPage<CoursePO> page);
}
