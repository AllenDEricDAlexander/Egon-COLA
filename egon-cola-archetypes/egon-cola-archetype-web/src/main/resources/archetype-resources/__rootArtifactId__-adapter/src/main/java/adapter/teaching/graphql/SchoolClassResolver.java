package ${package}.adapter.teaching.graphql;

import ${package}.application.teaching.command.AssignUserToClassCommand;
import ${package}.application.teaching.command.CreateGradeCommand;
import ${package}.application.teaching.command.CreateSchoolClassCommand;
import ${package}.application.teaching.manage.GradeManage;
import ${package}.application.teaching.manage.SchoolClassManage;
import ${package}.application.teaching.query.GradeDetailQuery;
import ${package}.application.teaching.query.SchoolClassDetailQuery;
import ${package}.application.teaching.result.GradeDetailResult;
import ${package}.application.teaching.result.SchoolClassDetailResult;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class SchoolClassResolver {

    private final GradeManage gradeManage;
    private final SchoolClassManage schoolClassManage;

    public SchoolClassResolver(GradeManage gradeManage, SchoolClassManage schoolClassManage) {
        this.gradeManage = gradeManage;
        this.schoolClassManage = schoolClassManage;
    }

    @QueryMapping
    public GradeDetailResult grade(@Argument String id) {
        return gradeManage.getGrade(new GradeDetailQuery(id));
    }

    @QueryMapping
    public SchoolClassDetailResult schoolClass(@Argument String id) {
        return schoolClassManage.getSchoolClass(new SchoolClassDetailQuery(id));
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
                new AssignUserToClassCommand(requestId(key), input.userId(), input.schoolClassId()));
        return true;
    }

    private static String requestId(String key) {
        return key == null || key.isBlank() ? UUID.randomUUID().toString() : key;
    }

    public record CreateGradeInput(String code, String name) {}
    public record CreateSchoolClassInput(String name, String gradeCode) {}
    public record AssignUserToSchoolClassInput(String userId, String schoolClassId) {}
}
