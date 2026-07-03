#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.convertor;

import ${package}.domain.entities.examing.ExamResult;
import ${package}.facade.dto.examing.ExamResultDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExamResultAdapterMapper extends BaseMapper<ExamResult, ExamResultDTO> {
    @Override
    @Mapping(target = "status", ignore = true)
    ExamResultDTO convert(ExamResult examResult);
}
