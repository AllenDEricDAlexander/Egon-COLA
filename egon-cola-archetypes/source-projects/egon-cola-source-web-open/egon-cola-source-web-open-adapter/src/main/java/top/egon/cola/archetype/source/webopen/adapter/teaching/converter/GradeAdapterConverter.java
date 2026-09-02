package top.egon.cola.archetype.source.webopen.adapter.teaching.converter;

import top.egon.cola.archetype.source.webopen.adapter.teaching.dto.CreateGradeRequest;
import top.egon.cola.archetype.source.webopen.adapter.teaching.vo.GradeDetailVO;
import top.egon.cola.archetype.source.webopen.application.teaching.command.CreateGradeCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.result.GradeDetailResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GradeAdapterConverter {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "code", source = "request.code")
    @Mapping(target = "name", source = "request.name")
    CreateGradeCommand toCommand(String requestId, CreateGradeRequest request);

    GradeDetailVO toVO(GradeDetailResult result);

    @BeforeMapping
    default void requireRequest(CreateGradeRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireResult(GradeDetailResult result) {
        Objects.requireNonNull(result, "result");
    }
}
