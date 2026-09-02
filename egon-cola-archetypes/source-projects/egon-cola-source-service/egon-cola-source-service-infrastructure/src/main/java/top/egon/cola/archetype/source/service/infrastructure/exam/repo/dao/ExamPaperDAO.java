package top.egon.cola.archetype.source.service.infrastructure.exam.repo.dao;

import top.egon.cola.archetype.source.service.infrastructure.exam.repo.po.ExamPaperPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.List;

@Mapper
public interface ExamPaperDAO extends EgonColaMapper<ExamPaperPO> {
    ExamPaperPO selectByExamId(@Param("examId") Long examId);
    List<ExamPaperPO> selectAllByExamId(@Param("examId") Long examId);
}
