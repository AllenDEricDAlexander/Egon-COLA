package ${package}.adapter.teaching.facade.impl;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.adapter.facade.impl.OrganizationIdBoundary;
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
    private final OrganizationFacadeSupport support;

    @Override public SchoolClassDetailDTO createSchoolClass(CreateSchoolClassDTO request) {
        return support.invoke(() -> toDTO(schoolClassManage.createSchoolClass(
                new CreateSchoolClassCommand(
                    support.requestId(), request.name(), request.gradeCode()))));
    }
    @Override public SchoolClassDetailDTO getSchoolClass(String gradeId, String schoolClassId) {
        return support.invoke(
                () -> toDTO(schoolClassManage.getSchoolClass(
                        new SchoolClassDetailQuery(OrganizationIdBoundary.parse(gradeId, "gradeId"),
                            OrganizationIdBoundary.parse(schoolClassId, "schoolClassId")))));
    }
    @Override public void assignUser(AssignUserToClassDTO request) {
        support.invoke(() -> schoolClassManage.assignUser(new AssignUserToClassCommand(
            support.requestId(),
            OrganizationIdBoundary.parse(request.gradeId(), "gradeId"),
            OrganizationIdBoundary.parse(request.schoolClassId(), "schoolClassId"),
            OrganizationIdBoundary.parse(request.userId(), "userId"))));
    }
    private static SchoolClassDetailDTO toDTO(SchoolClassDetailResult result) {
        return new SchoolClassDetailDTO(Long.toString(result.id()), result.name(), result.gradeCode(),
            result.gradeName(), result.status(), result.userIds().stream().map(String::valueOf).toList());
    }
}
