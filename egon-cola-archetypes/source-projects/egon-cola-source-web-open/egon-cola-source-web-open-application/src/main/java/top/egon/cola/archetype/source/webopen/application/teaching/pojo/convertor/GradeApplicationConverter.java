package top.egon.cola.archetype.source.webopen.application.teaching.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.CreateGradeCommand;

/** Command assembly for the grade create use case. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "gradeApplicationConverterImpl"))
public interface GradeApplicationConverter {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    CreateGradeCommand toCommand(String requestId, String code, String name);
}
