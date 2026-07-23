package ${package}.adapter.teaching.facade.impl;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.command.AssignUserToClassCommand;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.application.teaching.query.SchoolClassDetailQuery;
import ${package}.application.teaching.result.SchoolClassDetailResult;
import lombok.RequiredArgsConstructor;
import top.egon.cola.organization.facade.teaching.dto.AssignUserToClassDTO;
import top.egon.cola.organization.facade.teaching.dto.CreateSchoolClassDTO;
import top.egon.cola.organization.facade.teaching.dto.SchoolClassDetailDTO;
import top.egon.cola.organization.facade.teaching.SchoolClassFacade;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service("schoolClassFacade")
@Validated
@RequiredArgsConstructor
public class SchoolClassFacadeImpl implements SchoolClassFacade {
    private final SchoolClassManage schoolClassManage;

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
