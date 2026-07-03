package ${package}.adapter.convertor;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.facade.dto.teaching.SchoolClassDTO;
import io.github.linpeilie.BaseMapper;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("schoolClassAdapterConverter")
@RequiredArgsConstructor
public class SchoolClassAdapterConverter {
    @Qualifier("converter")
    private final Converter converter;

    public SchoolClassDTO toDto(SchoolClass schoolClass) {
        return converter.convert(schoolClass, SchoolClassDTO.class);
    }

    @Mapper(componentModel = "spring")
    public interface SchoolClassMapper extends BaseMapper<SchoolClass, SchoolClassDTO> {
    }
}
