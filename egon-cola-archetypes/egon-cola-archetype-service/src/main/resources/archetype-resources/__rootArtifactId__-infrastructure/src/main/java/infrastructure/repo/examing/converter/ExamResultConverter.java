#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.examing.converter;

import java.time.LocalDateTime;

import ${package}.domain.entities.examing.ExamResult;
import ${package}.domain.enums.ExamResultStatus;
import ${package}.infrastructure.repo.examing.po.ExamResultPo;
import org.springframework.stereotype.Component;

@Component
public class ExamResultConverter {

    public ExamResultPo toPo(ExamResult examResult, LocalDateTime createdAt, LocalDateTime updatedAt) {
        ExamResultPo examResultPo = new ExamResultPo();
        examResultPo.setId(examResult.getId());
        examResultPo.setCourseId(examResult.getCourseId());
        examResultPo.setStudentId(examResult.getStudentId());
        examResultPo.setScore(examResult.getScore());
        examResultPo.setStatus(examResult.getStatus().name());
        examResultPo.setCreatedAt(createdAt);
        examResultPo.setUpdatedAt(updatedAt);
        return examResultPo;
    }

    public ExamResult toDomain(ExamResultPo examResultPo) {
        ExamResult examResult = new ExamResult();
        examResult.setId(examResultPo.getId());
        examResult.setCourseId(examResultPo.getCourseId());
        examResult.setStudentId(examResultPo.getStudentId());
        examResult.setScore(examResultPo.getScore());
        examResult.setStatus(ExamResultStatus.valueOf(examResultPo.getStatus()));
        return examResult;
    }
}
