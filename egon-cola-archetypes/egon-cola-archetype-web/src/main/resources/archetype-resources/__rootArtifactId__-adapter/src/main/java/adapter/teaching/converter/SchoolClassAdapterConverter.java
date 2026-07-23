package ${package}.adapter.teaching.converter;

import ${package}.adapter.teaching.dto.CreateSchoolClassRequest;
import ${package}.adapter.teaching.vo.SchoolClassDetailVO;
import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.command.AssignUserToClassCommand;
import ${package}.application.teaching.result.SchoolClassDetailResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SchoolClassAdapterConverter {

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "gradeCode", source = "request.gradeCode")
    CreateSchoolClassCommand toCommand(String requestId, CreateSchoolClassRequest request);

    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "schoolClassId", source = "schoolClassId")
    @Mapping(target = "userId", source = "userId")
    AssignUserToClassCommand toCommand(String requestId, String schoolClassId, String userId);

    SchoolClassDetailVO toVO(SchoolClassDetailResult result);
}
