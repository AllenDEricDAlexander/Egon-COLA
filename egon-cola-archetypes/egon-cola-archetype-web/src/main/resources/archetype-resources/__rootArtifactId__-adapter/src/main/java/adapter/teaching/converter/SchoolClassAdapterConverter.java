package ${package}.adapter.teaching.converter;

import ${package}.adapter.teaching.dto.CreateSchoolClassRequest;
import ${package}.adapter.teaching.vo.SchoolClassDetailVO;
import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.command.AssignUserToClassCommand;
import ${package}.application.teaching.result.SchoolClassDetailResult;
import org.springframework.stereotype.Component;

@Component("schoolClassAdapterConverter")
public final class SchoolClassAdapterConverter {
    public CreateSchoolClassCommand toCommand(String requestId, CreateSchoolClassRequest request) {
        return new CreateSchoolClassCommand(requestId, request.name(), request.gradeCode());
    }
    public AssignUserToClassCommand toCommand(String requestId, String schoolClassId, String userId) {
        return new AssignUserToClassCommand(requestId, userId, schoolClassId);
    }
    public SchoolClassDetailVO toVO(SchoolClassDetailResult result) {
        return new SchoolClassDetailVO(result.id(), result.name(), result.gradeCode(),
            result.gradeName(), result.status(), result.userIds());
    }
}
