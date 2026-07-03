#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.examing.converter;

import ${package}.domain.entities.examing.ExamResult;
import ${package}.domain.enums.ExamResultStatus;
import ${package}.infrastructure.repo.examing.po.ExamResultPo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("examResultConverter")
@RequiredArgsConstructor
public class ExamResultConverter {

    @Qualifier("examResultPoMapperImpl")
    private final ExamResultPoMapper examResultPoMapper;

    @Qualifier("examResultDomainMapperImpl")
    private final ExamResultDomainMapper examResultDomainMapper;

    public ExamResultPo toPo(ExamResult examResult, LocalDateTime createdAt, LocalDateTime updatedAt) {
        ExamResultPo examResultPo = examResultPoMapper.convert(examResult);
        return new ExamResultPo(
                examResultPo.getId(),
                examResultPo.getCourseId(),
                examResultPo.getStudentId(),
                examResultPo.getScore(),
                examResult.getStatus().name(),
                createdAt,
                updatedAt);
    }

    public ExamResult toDomain(ExamResultPo examResultPo) {
        ExamResult examResult = examResultDomainMapper.convert(examResultPo);
        examResult.setStatus(ExamResultStatus.valueOf(examResultPo.getStatus()));
        return examResult;
    }
}
