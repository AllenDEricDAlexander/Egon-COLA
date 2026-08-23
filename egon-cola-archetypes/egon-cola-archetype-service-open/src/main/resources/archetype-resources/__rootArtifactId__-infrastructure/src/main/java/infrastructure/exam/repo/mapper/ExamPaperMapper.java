#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.mapper;

import ${package}.infrastructure.exam.repo.po.ExamPaperPo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface ExamPaperMapper extends BaseMapper<ExamPaperPo> {

    ExamPaperPo selectByExamIdAndId(
            @Param("examId") Long examId,
            @Param("id") Long id);

    ExamPaperPo selectByExamId(@Param("examId") Long examId);
}
