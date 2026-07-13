package ${package}.adapter.teaching.facade.impl;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.command.AssignUserToClassCommand;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.application.teaching.query.SchoolClassDetailQuery;
import ${package}.application.teaching.result.SchoolClassDetailResult;
import ${package}.facade.teaching.dto.AssignUserToClassDTO;
import ${package}.facade.teaching.dto.CreateSchoolClassDTO;
import ${package}.facade.teaching.dto.SchoolClassDetailDTO;
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
