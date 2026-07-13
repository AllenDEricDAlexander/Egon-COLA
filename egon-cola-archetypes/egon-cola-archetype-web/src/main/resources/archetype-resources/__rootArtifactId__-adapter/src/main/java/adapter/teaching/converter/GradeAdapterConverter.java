package ${package}.adapter.teaching.converter;

import ${package}.adapter.teaching.dto.CreateGradeRequest;
import ${package}.adapter.teaching.vo.GradeDetailVO;
import ${package}.application.teaching.command.CreateGradeCommand;
import ${package}.application.teaching.result.GradeDetailResult;
import org.springframework.stereotype.Component;

@Component("gradeAdapterConverter")
public final class GradeAdapterConverter {
    public CreateGradeCommand toCommand(String requestId, CreateGradeRequest request) {
        return new CreateGradeCommand(requestId, request.code(), request.name());
    }
    public GradeDetailVO toVO(GradeDetailResult result) {
        return new GradeDetailVO(result.id(), result.code(), result.name(), result.status());
    }
}
