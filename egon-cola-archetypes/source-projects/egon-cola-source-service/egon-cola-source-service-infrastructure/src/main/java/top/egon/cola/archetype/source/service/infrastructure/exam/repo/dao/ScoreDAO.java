package top.egon.cola.archetype.source.service.infrastructure.exam.repo.dao;

import top.egon.cola.archetype.source.service.infrastructure.exam.repo.po.ScorePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.List;

@Mapper
public interface ScoreDAO extends EgonColaMapper<ScorePO> {
    ScorePO selectByExamIdAndId(
            @Param("examId") Long examId, @Param("id") Long id);
    long countByExamIdAndStudentId(
            @Param("examId") Long examId, @Param("studentId") Long studentId);
    List<ScorePO> selectPageByExamId(
            @Param("examId") Long examId,
            @Param("limit") int limit,
            @Param("offset") int offset);
    long countByExamId(@Param("examId") Long examId);
}
