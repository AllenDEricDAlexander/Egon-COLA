package ${package}.adapter.facade.impl.teaching;

import ${package}.application.command.teaching.CreateSchoolClassCommand;
import ${package}.application.command.teaching.AssignUserToClassCommand;
import ${package}.application.manage.teaching.SchoolClassManage;
import ${package}.application.query.teaching.SchoolClassDetailQuery;
import ${package}.application.result.teaching.SchoolClassDetailResult;
import ${package}.facade.dto.teaching.AssignUserToClassDTO;
import ${package}.facade.dto.teaching.CreateSchoolClassDTO;
import ${package}.facade.dto.teaching.SchoolClassDetailDTO;
import ${package}.facade.teaching.SchoolClassFacade;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service("schoolClassFacade")
@Validated
public class SchoolClassFacadeImpl implements SchoolClassFacade {
    private final SchoolClassManage schoolClassManage;
    public SchoolClassFacadeImpl(SchoolClassManage schoolClassManage) { this.schoolClassManage = schoolClassManage; }

    @Override public SchoolClassDetailDTO createSchoolClass(CreateSchoolClassDTO request) {
        return toDTO(schoolClassManage.createSchoolClass(new CreateSchoolClassCommand(
            UUID.randomUUID().toString(), request.name(), request.gradeCode())));
    }
    @Override public SchoolClassDetailDTO getSchoolClass(String schoolClassId) {
        return toDTO(schoolClassManage.getSchoolClass(new SchoolClassDetailQuery(schoolClassId)));
    }
    @Override public void assignUser(AssignUserToClassDTO request) {
        schoolClassManage.assignUser(new AssignUserToClassCommand(
            UUID.randomUUID().toString(), request.userId(), request.schoolClassId()));
    }
    private static SchoolClassDetailDTO toDTO(SchoolClassDetailResult result) {
        return new SchoolClassDetailDTO(result.id(), result.name(), result.gradeCode(),
            result.gradeName(), result.status(), result.userIds());
    }
}
