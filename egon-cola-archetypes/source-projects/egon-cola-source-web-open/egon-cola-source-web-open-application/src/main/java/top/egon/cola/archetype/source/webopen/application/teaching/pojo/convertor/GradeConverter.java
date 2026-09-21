package top.egon.cola.archetype.source.webopen.application.teaching.pojo.convertor;

import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.result.GradeDetailResult;
import top.egon.cola.archetype.source.webopen.domain.teaching.entities.Grade;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

/** Grade entity-to-result projection; the value object is read through its record accessor. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "gradeConverterImpl"))
public interface GradeConverter extends BaseForwardConverter<Grade, GradeDetailResult> {

    @Override
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code.value")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "status", source = "status")
    GradeDetailResult toTarget(Grade source);

    /** Callable contract kept from the previous hand-written projection. */
    default GradeDetailResult toResult(Grade source) {
        return toTarget(source);
    }
}
