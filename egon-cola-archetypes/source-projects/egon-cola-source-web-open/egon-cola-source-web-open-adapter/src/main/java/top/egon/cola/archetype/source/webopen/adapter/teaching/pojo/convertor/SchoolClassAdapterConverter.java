package top.egon.cola.archetype.source.webopen.adapter.teaching.pojo.convertor;

import top.egon.cola.archetype.source.webopen.adapter.teaching.pojo.dto.CreateSchoolClassRequest;
import top.egon.cola.archetype.source.webopen.adapter.teaching.pojo.vo.SchoolClassDetailVO;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.result.SchoolClassDetailResult;
import org.mapstruct.BeforeMapping;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Objects;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "schoolClassAdapterConverterImpl"))
public interface SchoolClassAdapterConverter extends BaseForwardConverter<SchoolClassDetailResult, SchoolClassDetailVO> {

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

    @Override
    SchoolClassDetailVO toTarget(SchoolClassDetailResult source);

    /** Callable contract kept from the previous hand-written projection. */
    default SchoolClassDetailVO toVO(SchoolClassDetailResult result) {
        return toTarget(result);
    }

    @BeforeMapping
    default void requireRequest(CreateSchoolClassRequest request) {
        Objects.requireNonNull(request, "request");
    }

    @BeforeMapping
    default void requireResult(SchoolClassDetailResult result) {
        Objects.requireNonNull(result, "result");
    }
}
