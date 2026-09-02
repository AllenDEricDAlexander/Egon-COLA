package top.egon.cola.archetype.source.web.adapter.teaching.graphql;

import top.egon.cola.archetype.source.web.application.teaching.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.command.CreateGradeCommand;
import top.egon.cola.archetype.source.web.application.teaching.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.web.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.web.application.teaching.query.GradeDetailQuery;
import top.egon.cola.archetype.source.web.application.teaching.query.SchoolClassDetailQuery;
import top.egon.cola.archetype.source.web.application.teaching.result.GradeDetailResult;
import top.egon.cola.archetype.source.web.application.teaching.result.SchoolClassDetailResult;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class SchoolClassResolver {

    private final GradeManage gradeManage;
    private final SchoolClassManage schoolClassManage;

    @QueryMapping
    public GradeDetailResult grade(@Argument String id) {
        return gradeManage.getGrade(new GradeDetailQuery(
                OrganizationFacadeSupport.positiveId(id, "gradeId")));
    }

    @QueryMapping
    public SchoolClassDetailResult schoolClass(@Argument String gradeId, @Argument String id) {
        return schoolClassManage.getSchoolClass(new SchoolClassDetailQuery(
                OrganizationFacadeSupport.positiveId(gradeId, "gradeId"),
                OrganizationFacadeSupport.positiveId(id, "schoolClassId")));
    }

    @MutationMapping
    public GradeDetailResult createGrade(
            @Argument CreateGradeInput input,
            @ContextValue(name = "idempotencyKey", required = false) String key) {
        return gradeManage.createGrade(new CreateGradeCommand(requestId(key), input.code(), input.name()));
    }

    @MutationMapping
    public SchoolClassDetailResult createSchoolClass(
            @Argument CreateSchoolClassInput input,
            @ContextValue(name = "idempotencyKey", required = false) String key) {
        return schoolClassManage.createSchoolClass(
                new CreateSchoolClassCommand(requestId(key), input.name(), input.gradeCode()));
    }

    @MutationMapping
    public boolean assignUserToSchoolClass(
            @Argument AssignUserToSchoolClassInput input,
            @ContextValue(name = "idempotencyKey", required = false) String key) {
        schoolClassManage.assignUser(
                new AssignUserToClassCommand(
                        requestId(key),
                        OrganizationFacadeSupport.positiveId(input.gradeId(), "gradeId"),
                        OrganizationFacadeSupport.positiveId(input.schoolClassId(), "schoolClassId"),
                        OrganizationFacadeSupport.positiveId(input.userId(), "userId")));
        return true;
    }

    private static String requestId(String key) {
        return key == null || key.isBlank() ? UUID.randomUUID().toString() : key;
    }

    public record CreateGradeInput(String code, String name) {}
    public record CreateSchoolClassInput(String name, String gradeCode) {}
    public record AssignUserToSchoolClassInput(String gradeId, String userId, String schoolClassId) {}
}
