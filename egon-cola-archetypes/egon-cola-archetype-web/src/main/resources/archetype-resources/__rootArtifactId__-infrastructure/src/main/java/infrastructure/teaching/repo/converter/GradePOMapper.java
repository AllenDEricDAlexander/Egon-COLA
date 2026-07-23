package ${package}.infrastructure.teaching.repo.converter;

import ${package}.domain.teaching.entities.Grade;
import ${package}.infrastructure.teaching.repo.po.GradePO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        imports = LocalDateTime.class)
public interface GradePOMapper extends BaseMapper<Grade, GradePO> {

    @Override
    @Mapping(target = "code", source = "code.value")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    GradePO convert(Grade source);
}
