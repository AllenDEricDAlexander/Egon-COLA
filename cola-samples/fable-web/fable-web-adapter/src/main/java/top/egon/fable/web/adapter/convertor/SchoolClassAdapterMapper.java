package top.egon.fable.web.adapter.convertor;

import top.egon.fable.web.domain.entities.teaching.SchoolClass;
import top.egon.fable.web.facade.dto.teaching.SchoolClassDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SchoolClassAdapterMapper extends BaseMapper<SchoolClass, SchoolClassDTO> {
}
