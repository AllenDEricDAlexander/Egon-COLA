package top.egon.cola.archetype.source.webopen.infrastructure.teaching.repo.dao;

import top.egon.cola.archetype.source.webopen.infrastructure.teaching.repo.po.SchoolClassPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface SchoolClassDAO extends EgonColaMapper<SchoolClassPO> {
    SchoolClassPO selectByGradeIdAndId(@Param("gradeId") Long gradeId, @Param("id") Long id);

    long countByGradeIdAndNameIgnoreCase(
            @Param("gradeId") Long gradeId, @Param("name") String name);
}
