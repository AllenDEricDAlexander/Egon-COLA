package top.egon.cola.archetype.source.web.adapter.teaching.graphql;

import lombok.extern.slf4j.Slf4j;
import java.util.Set;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties;
import top.egon.cola.platform.tianquan.jianshen.starter.field.FieldWriteAuthorizationCommand;
import top.egon.cola.platform.tianquan.jianshen.starter.field.Rbac3FieldWriteAuthorizationComponent;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RBACAPIResource;
import org.springframework.security.access.prepost.PreAuthorize;
import top.egon.cola.archetype.source.web.application.teaching.pojo.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.pojo.command.CreateGradeCommand;
import top.egon.cola.archetype.source.web.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.web.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.web.application.teaching.pojo.query.GradeDetailQuery;
import top.egon.cola.archetype.source.web.application.teaching.pojo.query.SchoolClassDetailQuery;
import top.egon.cola.archetype.source.web.application.teaching.pojo.result.GradeDetailResult;
import top.egon.cola.archetype.source.web.application.teaching.pojo.result.SchoolClassDetailResult;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;

@Controller("schoolClassResolver")
@RequiredArgsConstructor
@Slf4j
public class SchoolClassResolver {
    @Qualifier("rbac3FieldWriteAuthorizationComponent")
    private final Rbac3FieldWriteAuthorizationComponent fieldWrite;
    @Qualifier("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties")
    private final Rbac3StarterProperties authorizationProperties;


    @Qualifier("gradeManage")
    private final GradeManage gradeManage;
    @Qualifier("schoolClassManage")
    private final SchoolClassManage schoolClassManage;

    @QueryMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:grade:read')")
    @RBACAPIResource(code = "egon-cola-source-web.graphql.query.grade", permission = "egon-cola-source-web:grade:read", name = "Grade")
    public GradeDetailResult grade(@Argument String id) {
        return gradeManage.getGrade(new GradeDetailQuery(
                OrganizationFacadeSupport.positiveId(id, "gradeId")));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:school-class:read')")
    @RBACAPIResource(code = "egon-cola-source-web.graphql.query.schoolClass", permission = "egon-cola-source-web:school-class:read", name = "School class")
    public SchoolClassDetailResult schoolClass(@Argument String gradeId, @Argument String id) {
        return schoolClassManage.getSchoolClass(new SchoolClassDetailQuery(
                OrganizationFacadeSupport.positiveId(gradeId, "gradeId"),
                OrganizationFacadeSupport.positiveId(id, "schoolClassId")));
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:grade:create')")
    @RBACAPIResource(code = "egon-cola-source-web.graphql.mutation.createGrade", permission = "egon-cola-source-web:grade:create", name = "Create grade")
    public GradeDetailResult createGrade(
            @Argument CreateGradeInput input,
            @ContextValue(name = "idempotencyKey", required = false) String key) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-web:grade:create", authorizationProperties.getSystemCode(),
                "egon-cola-source-web.graphql.mutation.createGrade", Set.of("code","name")));
        return gradeManage.createGrade(new CreateGradeCommand(requestId(key), input.code(), input.name()));
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:school-class:create')")
    @RBACAPIResource(code = "egon-cola-source-web.graphql.mutation.createSchoolClass", permission = "egon-cola-source-web:school-class:create", name = "Create school class")
    public SchoolClassDetailResult createSchoolClass(
            @Argument CreateSchoolClassInput input,
            @ContextValue(name = "idempotencyKey", required = false) String key) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-web:school-class:create", authorizationProperties.getSystemCode(),
                "egon-cola-source-web.graphql.mutation.createSchoolClass", Set.of("name","gradeCode")));
        return schoolClassManage.createSchoolClass(
                new CreateSchoolClassCommand(requestId(key), input.name(), input.gradeCode()));
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:school-class:assign-user')")
    @RBACAPIResource(code = "egon-cola-source-web.graphql.mutation.assignUserToSchoolClass", permission = "egon-cola-source-web:school-class:assign-user", name = "Assign user to school class")
    public boolean assignUserToSchoolClass(
            @Argument AssignUserToSchoolClassInput input,
            @ContextValue(name = "idempotencyKey", required = false) String key) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-web:school-class:assign-user", authorizationProperties.getSystemCode(),
                "egon-cola-source-web.graphql.mutation.assignUserToSchoolClass", Set.of("userId")));
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
