package ${package}.infrastructure.repo.teaching.converter;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.infrastructure.repo.teaching.po.SchoolClassPo;
import io.github.linpeilie.BaseMapper;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("schoolClassPoConverter")
@RequiredArgsConstructor
public class SchoolClassPoConverter {
    @Qualifier("converter")
    private final Converter converter;

    public SchoolClassPo toPo(SchoolClass schoolClass) {
        SchoolClassPo schoolClassPo = converter.convert(schoolClass, SchoolClassPo.class);
        return new SchoolClassPo(
                schoolClassPo.getId(),
                schoolClassPo.getName(),
                schoolClassPo.getGradeName(),
                LocalDateTime.now());
    }

    public SchoolClass toEntity(SchoolClassPo schoolClassPo, List<String> userIds) {
        return SchoolClass.restore(
                schoolClassPo.getId(),
                schoolClassPo.getName(),
                schoolClassPo.getGradeName(),
                userIds);
    }

    @Mapper(componentModel = "spring")
    public interface SchoolClassMapper extends BaseMapper<SchoolClass, SchoolClassPo> {
        @Override
        @Mapping(target = "createdAt", ignore = true)
        SchoolClassPo convert(SchoolClass schoolClass);
    }
}
