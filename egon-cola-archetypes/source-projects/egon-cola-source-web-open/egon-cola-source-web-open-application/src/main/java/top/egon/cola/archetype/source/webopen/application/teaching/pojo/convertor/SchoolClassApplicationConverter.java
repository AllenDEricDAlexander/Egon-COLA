package top.egon.cola.archetype.source.webopen.application.teaching.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.CreateSchoolClassCommand;

/** Command assembly for the school class create use case. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class,
        elements = @AnnotateWith.Element(strings = "schoolClassApplicationConverterImpl"))
public interface SchoolClassApplicationConverter {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "gradeCode", source = "gradeCode")
    CreateSchoolClassCommand toCommand(String requestId, String name, String gradeCode);
}
