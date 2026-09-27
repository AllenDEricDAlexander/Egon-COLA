package top.egon.cola.archetype.source.web.infrastructure.teaching.dao;

import top.egon.cola.archetype.source.web.infrastructure.teaching.po.GradePO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.validation.annotation.Validated;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
@Validated
public interface GradeDAO extends EgonColaMapper<GradePO> {
    GradePO selectByCode(@Param("code") String code);

    long countByCode(@Param("code") String code);
}
