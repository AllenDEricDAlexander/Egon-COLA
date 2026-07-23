package ${package}.infrastructure.teaching.repo.converter;

import ${package}.domain.teaching.entities.Course;
import ${package}.infrastructure.teaching.repo.po.CoursePO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.Objects;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        imports = Instant.class)
public interface CoursePOMapper extends BaseMapper<Course, CoursePO> {

    @Override
    @Mapping(target = "courseCode", source = "code.value")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    CoursePO convert(Course source);

    @Override
    @Mapping(target = "courseCode", source = "code.value")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    CoursePO convert(Course source, @MappingTarget CoursePO target);

    @BeforeMapping
    default void requireSource(Course source) {
        Objects.requireNonNull(source, "source");
    }
}
