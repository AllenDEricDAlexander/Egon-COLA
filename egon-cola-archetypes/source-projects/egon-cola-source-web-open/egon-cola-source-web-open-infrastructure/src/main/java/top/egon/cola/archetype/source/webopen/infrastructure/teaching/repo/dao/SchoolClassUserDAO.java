package top.egon.cola.archetype.source.webopen.infrastructure.teaching.repo.dao;

import top.egon.cola.archetype.source.webopen.infrastructure.teaching.repo.po.SchoolClassUserPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.List;

@Mapper
public interface SchoolClassUserDAO extends EgonColaMapper<SchoolClassUserPO> {
    List<SchoolClassUserPO> selectByGradeIdAndSchoolClassId(
            @Param("gradeId") Long gradeId, @Param("schoolClassId") Long schoolClassId);

    long countByGradeIdAndSchoolClassIdAndUserId(
            @Param("gradeId") Long gradeId,
            @Param("schoolClassId") Long schoolClassId,
            @Param("userId") Long userId);
}
