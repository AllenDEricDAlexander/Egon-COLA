package ${package}.infrastructure.teaching.repo.dao;

import ${package}.infrastructure.teaching.repo.po.GradePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface GradeDAO extends EgonColaMapper<GradePO> {
    GradePO selectByCode(@Param("code") String code);

    long countByCode(@Param("code") String code);
}
