package top.egon.cola.archetype.source.webopen.adapter.teaching.pojo.convertor;

import top.egon.cola.archetype.source.webopen.adapter.teaching.pojo.dto.CreateGradeRequest;
import top.egon.cola.archetype.source.webopen.adapter.teaching.pojo.vo.GradeDetailVO;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.CreateGradeCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.result.GradeDetailResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Objects;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "gradeAdapterConverterImpl"))
public interface GradeAdapterConverter extends BaseForwardConverter<GradeDetailResult, GradeDetailVO> {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "code", source = "request.code")
    @Mapping(target = "name", source = "request.name")
    CreateGradeCommand toCommand(String requestId, CreateGradeRequest request);

    @Override
    GradeDetailVO toTarget(GradeDetailResult source);

    /** Callable contract kept from the previous hand-written projection. */
    default GradeDetailVO toVO(GradeDetailResult result) {
        return toTarget(result);
    }

    @BeforeMapping
    default void requireRequest(CreateGradeRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireResult(GradeDetailResult result) {
        Objects.requireNonNull(result, "result");
    }
}
