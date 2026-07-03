package ${package}.adapter.convertor;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.facade.dto.teaching.SchoolClassDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SchoolClassAdapterMapper extends BaseMapper<SchoolClass, SchoolClassDTO> {
}
