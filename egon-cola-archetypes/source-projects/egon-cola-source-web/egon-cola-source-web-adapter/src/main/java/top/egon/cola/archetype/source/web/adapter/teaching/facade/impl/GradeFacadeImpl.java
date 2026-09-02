package top.egon.cola.archetype.source.web.adapter.teaching.facade.impl;

import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.web.application.teaching.command.CreateGradeCommand;
import top.egon.cola.archetype.source.web.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.web.application.teaching.query.GradeDetailQuery;
import top.egon.cola.archetype.source.web.application.teaching.result.GradeDetailResult;
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
    @Override public GradeDetailDTO getGrade(Long gradeId) {
        return OrganizationFacadeSupport.invoke(() -> toDTO(gradeManage.getGrade(new GradeDetailQuery(gradeId))));
    }
    private static GradeDetailDTO toDTO(GradeDetailResult result) {
        return new GradeDetailDTO(result.id(), result.code(), result.name(), result.status());
    }
}
