package ${package}.adapter.teaching.facade.impl;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.adapter.facade.impl.OrganizationIdBoundary;
import ${package}.application.teaching.command.CreateGradeCommand;
import ${package}.application.teaching.manage.GradeManage;
import ${package}.application.teaching.query.GradeDetailQuery;
import ${package}.application.teaching.result.GradeDetailResult;
import lombok.RequiredArgsConstructor;
import top.egon.cola.organization.facade.teaching.dto.CreateGradeDTO;
import top.egon.cola.organization.facade.teaching.dto.GradeDetailDTO;
import top.egon.cola.organization.facade.teaching.GradeFacade;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service("gradeFacade")
@Validated
@RequiredArgsConstructor
public class GradeFacadeImpl implements GradeFacade {
    private final GradeManage gradeManage;
    private final OrganizationFacadeSupport support;

    @Override public GradeDetailDTO createGrade(CreateGradeDTO request) {
        return support.invoke(() -> toDTO(gradeManage.createGrade(new CreateGradeCommand(
            support.requestId(), request.code(), request.name()))));
    }
    @Override public GradeDetailDTO getGrade(String gradeId) {
        return support.invoke(() -> toDTO(gradeManage.getGrade(new GradeDetailQuery(
            OrganizationIdBoundary.parse(gradeId, "gradeId")))));
    }
    private static GradeDetailDTO toDTO(GradeDetailResult result) {
        return new GradeDetailDTO(Long.toString(result.id()), result.code(), result.name(), result.status());
    }
}
