package ${package}.adapter.teaching.facade.impl;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
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

    @Override public GradeDetailDTO createGrade(CreateGradeDTO request) {
        return OrganizationFacadeSupport.invoke(() -> toDTO(gradeManage.createGrade(new CreateGradeCommand(
            OrganizationFacadeSupport.requestId(), request.code(), request.name()))));
    }
    @Override public GradeDetailDTO getGrade(String gradeId) {
        return OrganizationFacadeSupport.invoke(() -> toDTO(gradeManage.getGrade(new GradeDetailQuery(gradeId))));
    }
    private static GradeDetailDTO toDTO(GradeDetailResult result) {
        return new GradeDetailDTO(result.id(), result.code(), result.name(), result.status());
    }
}
