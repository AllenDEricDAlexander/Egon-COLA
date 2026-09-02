package top.egon.cola.archetype.source.web.adapter.teaching.converter;

import top.egon.cola.archetype.source.web.adapter.teaching.dto.CreateSchoolClassRequest;
import top.egon.cola.archetype.source.web.adapter.teaching.vo.SchoolClassDetailVO;
import top.egon.cola.archetype.source.web.application.teaching.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.result.SchoolClassDetailResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Objects;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SchoolClassAdapterConverter {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "gradeCode", source = "request.gradeCode")
    CreateSchoolClassCommand toCommand(String requestId, CreateSchoolClassRequest request);

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "gradeId", source = "gradeId")
    @Mapping(target = "schoolClassId", source = "schoolClassId")
    @Mapping(target = "userId", source = "userId")
    AssignUserToClassCommand toCommand(
            String requestId,
            Long gradeId,
            Long schoolClassId,
            Long userId);

    SchoolClassDetailVO toVO(SchoolClassDetailResult result);

    @BeforeMapping
    default void requireRequest(CreateSchoolClassRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireResult(SchoolClassDetailResult result) {
        Objects.requireNonNull(result, "result");
    }
}
