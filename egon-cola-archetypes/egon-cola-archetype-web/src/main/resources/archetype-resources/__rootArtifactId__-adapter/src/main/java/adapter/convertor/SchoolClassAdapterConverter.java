package ${package}.adapter.convertor;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.facade.dto.teaching.SchoolClassDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("schoolClassAdapterConverter")
@RequiredArgsConstructor
public class SchoolClassAdapterConverter {
    @Qualifier("schoolClassAdapterMapperImpl")
    private final SchoolClassAdapterMapper schoolClassAdapterMapper;

    public SchoolClassDTO toDto(SchoolClass schoolClass) {
        return schoolClassAdapterMapper.convert(schoolClass);
    }
}
