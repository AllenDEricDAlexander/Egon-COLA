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
public interface ExamResultPoMapper extends BaseMapper<ExamResult, ExamResultPo> {

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
