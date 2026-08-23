#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.exam.repo.mapper;

import ${package}.infrastructure.exam.repo.po.ScorePo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ScoreMapper extends BaseMapper<ScorePo> {

    ScorePo selectByExamIdAndId(
            @Param("examId") Long examId,
            @Param("id") Long id);

    long countByExamIdAndStudentId(
            @Param("examId") Long examId,
            @Param("studentId") Long studentId);

    List<ScorePo> selectPageByExamId(
            @Param("examId") Long examId,
            @Param("offset") long offset,
            @Param("limit") int limit);

    long countByExamId(@Param("examId") Long examId);
}
