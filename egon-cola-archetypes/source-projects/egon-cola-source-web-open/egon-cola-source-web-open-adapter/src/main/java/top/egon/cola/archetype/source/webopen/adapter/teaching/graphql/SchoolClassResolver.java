package top.egon.cola.archetype.source.webopen.adapter.teaching.graphql;

import top.egon.cola.archetype.source.webopen.application.teaching.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.command.CreateGradeCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.webopen.application.teaching.query.GradeDetailQuery;
import top.egon.cola.archetype.source.webopen.application.teaching.query.SchoolClassDetailQuery;
import top.egon.cola.archetype.source.webopen.application.teaching.result.GradeDetailResult;
import top.egon.cola.archetype.source.webopen.application.teaching.result.SchoolClassDetailResult;
import top.egon.cola.archetype.source.webopen.adapter.facade.impl.OrganizationIdBoundary;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import top.egon.cola.component.common.id.generator.LongIdGenerator;

@Controller
@RequiredArgsConstructor
public class SchoolClassResolver {

    private final GradeManage gradeManage;
    private final SchoolClassManage schoolClassManage;
    private final LongIdGenerator idGenerator;

    @QueryMapping
    public GradeDetailResult grade(@Argument String id) {
        return gradeManage.getGrade(new GradeDetailQuery(OrganizationIdBoundary.parse(id, "gradeId")));
    }

    @QueryMapping
    public SchoolClassDetailResult schoolClass(@Argument String gradeId, @Argument String id) {
        return schoolClassManage.getSchoolClass(new SchoolClassDetailQuery(
            OrganizationIdBoundary.parse(gradeId, "gradeId"),
            OrganizationIdBoundary.parse(id, "schoolClassId")));
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
                        requestId(key), OrganizationIdBoundary.parse(input.gradeId(), "gradeId"),
                        OrganizationIdBoundary.parse(input.schoolClassId(), "schoolClassId"),
                        OrganizationIdBoundary.parse(input.userId(), "userId")));
        return true;
    }

    private String requestId(String key) {
        return key == null || key.isBlank() ? Long.toString(idGenerator.nextLongId()) : key;
    }

    public record CreateGradeInput(String code, String name) {}
    public record CreateSchoolClassInput(String name, String gradeCode) {}
    public record AssignUserToSchoolClassInput(String gradeId, String userId, String schoolClassId) {}
}
