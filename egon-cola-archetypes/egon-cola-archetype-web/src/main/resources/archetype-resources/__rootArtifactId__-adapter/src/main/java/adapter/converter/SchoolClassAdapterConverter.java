package ${package}.adapter.converter;

import ${package}.adapter.dto.teaching.CreateSchoolClassRequest;
import ${package}.adapter.vo.teaching.SchoolClassDetailVO;
import ${package}.application.command.teaching.CreateSchoolClassCommand;
import ${package}.application.result.teaching.SchoolClassDetailResult;
import org.springframework.stereotype.Component;

@Component("schoolClassAdapterConverter")
public final class SchoolClassAdapterConverter {
    public CreateSchoolClassCommand toCommand(String requestId, CreateSchoolClassRequest request) {
        return new CreateSchoolClassCommand(requestId, request.name(), request.gradeCode());
    }
    public SchoolClassDetailVO toVO(SchoolClassDetailResult result) {
        return new SchoolClassDetailVO(result.id(), result.name(), result.gradeCode(),
            result.gradeName(), result.status(), result.userIds());
    }
}
