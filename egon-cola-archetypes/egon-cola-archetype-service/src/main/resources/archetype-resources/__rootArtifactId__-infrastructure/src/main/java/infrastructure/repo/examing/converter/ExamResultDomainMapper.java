#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.examing.converter;

import ${package}.domain.entities.examing.ExamResult;
import ${package}.infrastructure.repo.examing.po.ExamResultPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExamResultDomainMapper extends BaseMapper<ExamResultPo, ExamResult> {

    @Override
    @Mapping(target = "status", ignore = true)
    ExamResult convert(ExamResultPo examResultPo);

    @Override
    @Mapping(target = "status", ignore = true)
    ExamResult convert(ExamResultPo examResultPo, @MappingTarget ExamResult examResult);
}
