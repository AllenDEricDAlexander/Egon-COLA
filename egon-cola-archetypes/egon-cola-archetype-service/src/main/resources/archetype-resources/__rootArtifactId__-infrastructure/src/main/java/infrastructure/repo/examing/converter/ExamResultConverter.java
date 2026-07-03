#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.examing.converter;

import ${package}.domain.entities.examing.ExamResult;
import ${package}.domain.enums.ExamResultStatus;
import ${package}.infrastructure.repo.examing.po.ExamResultPo;
import io.github.linpeilie.BaseMapper;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("examResultConverter")
@RequiredArgsConstructor
public class ExamResultConverter {

    @Qualifier("converter")
    private final Converter converter;

    public ExamResultPo toPo(ExamResult examResult, LocalDateTime createdAt, LocalDateTime updatedAt) {
        ExamResultPo examResultPo = converter.convert(examResult, ExamResultPo.class);
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
        ExamResult examResult = converter.convert(examResultPo, ExamResult.class);
        examResult.setStatus(ExamResultStatus.valueOf(examResultPo.getStatus()));
        return examResult;
    }

    @Mapper(componentModel = "spring")
    public interface ExamResultMapper extends BaseMapper<ExamResult, ExamResultPo> {

        @Override
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "updatedAt", ignore = true)
        @Mapping(target = "status", ignore = true)
        ExamResultPo convert(ExamResult examResult);

        @Override
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "updatedAt", ignore = true)
        @Mapping(target = "status", ignore = true)
        ExamResultPo convert(ExamResult examResult, @MappingTarget ExamResultPo examResultPo);
    }

    @Mapper(componentModel = "spring")
    public interface ExamResultDomainMapper extends BaseMapper<ExamResultPo, ExamResult> {

        @Override
        @Mapping(target = "status", ignore = true)
        ExamResult convert(ExamResultPo examResultPo);

        @Override
        @Mapping(target = "status", ignore = true)
        ExamResult convert(ExamResultPo examResultPo, @MappingTarget ExamResult examResult);
    }
}
