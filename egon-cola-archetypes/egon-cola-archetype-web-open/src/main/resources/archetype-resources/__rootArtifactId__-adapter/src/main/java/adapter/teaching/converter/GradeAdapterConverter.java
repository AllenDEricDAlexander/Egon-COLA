package ${package}.adapter.teaching.converter;

import ${package}.adapter.teaching.dto.CreateGradeRequest;
import ${package}.adapter.teaching.vo.GradeDetailVO;
import ${package}.application.teaching.command.CreateGradeCommand;
import ${package}.application.teaching.result.GradeDetailResult;
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
