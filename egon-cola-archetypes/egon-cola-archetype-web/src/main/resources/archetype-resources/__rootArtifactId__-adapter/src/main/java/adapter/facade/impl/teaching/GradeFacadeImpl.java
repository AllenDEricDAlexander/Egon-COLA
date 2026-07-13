package ${package}.adapter.facade.impl.teaching;

import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import ${package}.application.command.teaching.CreateGradeCommand;
import ${package}.application.manage.teaching.GradeManage;
import ${package}.application.query.teaching.GradeDetailQuery;
import ${package}.application.result.teaching.GradeDetailResult;
import ${package}.facade.teaching.dto.CreateGradeDTO;
import ${package}.facade.teaching.dto.GradeDetailDTO;
import ${package}.facade.teaching.GradeFacade;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service("gradeFacade")
@Validated
public class GradeFacadeImpl implements GradeFacade {
    private final GradeManage gradeManage;
    public GradeFacadeImpl(GradeManage gradeManage) { this.gradeManage = gradeManage; }

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
