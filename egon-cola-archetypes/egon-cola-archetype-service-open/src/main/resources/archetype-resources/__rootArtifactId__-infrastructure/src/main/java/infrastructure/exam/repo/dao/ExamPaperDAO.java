#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.dao;

import ${package}.infrastructure.exam.repo.po.ExamPaperPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.List;

@Mapper
public interface ExamPaperDAO extends EgonColaMapper<ExamPaperPO> {
    ExamPaperPO selectByExamId(@Param("examId") Long examId);
    List<ExamPaperPO> selectAllByExamId(@Param("examId") Long examId);
}
