package ${package}.adapter.facade.impl.teaching;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
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

@Service("schoolClassFacade")
@Validated
public class SchoolClassFacadeImpl implements SchoolClassFacade {
    private final SchoolClassManage schoolClassManage;
    public SchoolClassFacadeImpl(SchoolClassManage schoolClassManage) { this.schoolClassManage = schoolClassManage; }

    @Override public SchoolClassDetailDTO createSchoolClass(CreateSchoolClassDTO request) {
        return OrganizationFacadeSupport.invoke(() -> toDTO(schoolClassManage.createSchoolClass(
                new CreateSchoolClassCommand(
                    OrganizationFacadeSupport.requestId(), request.name(), request.gradeCode()))));
    }
    @Override public SchoolClassDetailDTO getSchoolClass(String schoolClassId) {
        return OrganizationFacadeSupport.invoke(
                () -> toDTO(schoolClassManage.getSchoolClass(new SchoolClassDetailQuery(schoolClassId))));
    }
    @Override public void assignUser(AssignUserToClassDTO request) {
        OrganizationFacadeSupport.invoke(() -> schoolClassManage.assignUser(new AssignUserToClassCommand(
            OrganizationFacadeSupport.requestId(), request.userId(), request.schoolClassId())));
    }
    private static SchoolClassDetailDTO toDTO(SchoolClassDetailResult result) {
        return new SchoolClassDetailDTO(result.id(), result.name(), result.gradeCode(),
            result.gradeName(), result.status(), result.userIds());
    }
}
