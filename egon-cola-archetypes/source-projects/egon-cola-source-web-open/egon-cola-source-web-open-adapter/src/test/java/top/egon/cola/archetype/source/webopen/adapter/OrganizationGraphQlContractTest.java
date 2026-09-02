package top.egon.cola.archetype.source.webopen.adapter;

import top.egon.cola.archetype.source.webopen.adapter.teaching.graphql.SchoolClassResolver;
import top.egon.cola.archetype.source.webopen.adapter.user.graphql.UserResolver;
import top.egon.cola.archetype.source.webopen.adapter.handler.OrganizationGraphQlExceptionResolver;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.GradeManage;
import top.egon.cola.archetype.source.webopen.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.webopen.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.webopen.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.webopen.application.user.manage.UserManage;
import top.egon.cola.archetype.source.webopen.application.user.query.UserDetailQuery;
import top.egon.cola.archetype.source.webopen.application.teaching.query.SchoolClassDetailQuery;
import top.egon.cola.archetype.source.webopen.application.teaching.result.GradeDetailResult;
import top.egon.cola.archetype.source.webopen.application.teaching.result.SchoolClassDetailResult;
import top.egon.cola.archetype.source.webopen.application.user.result.UserDetailResult;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.application.exceptions.OrganizationFailureType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@GraphQlTest
@Import({UserResolver.class, SchoolClassResolver.class, OrganizationGraphQlExceptionResolver.class})
class OrganizationGraphQlContractTest {

    @Autowired
    private ExecutionGraphQlService executionGraphQlService;

    private GraphQlTester graphQlTester;

    @MockitoBean
    private UserManage userManage;
    @MockitoBean
    private RoleManage roleManage;
    @MockitoBean
    private PermissionManage permissionManage;
    @MockitoBean
    private GradeManage gradeManage;
    @MockitoBean
    private SchoolClassManage schoolClassManage;
    @MockitoBean
    private LongIdGenerator idGenerator;

    @BeforeEach
    void setUp() {
        graphQlTester = ExecutionGraphQlServiceTester.create(executionGraphQlService);
    }

    @Test
    void exposesBothDomainQueriesAndMutations() {
        when(gradeManage.createGrade(any()))
                .thenReturn(new GradeDetailResult(1001L, "GRADE_ONE", "Grade One", "ACTIVE"));
        when(userManage.getUser(new UserDetailQuery(2001L)))
                .thenReturn(new UserDetailResult(2001L, "Mario", "mario@example.com", "ACTIVE", List.of()));
        when(schoolClassManage.getSchoolClass(new SchoolClassDetailQuery(1001L, 3001L)))
                .thenReturn(new SchoolClassDetailResult(
                        3001L, "Class One", "GRADE_ONE", "Grade One", "ACTIVE", List.of()));

        graphQlTester.document("mutation { createGrade(input:{code:\"GRADE_ONE\",name:\"Grade One\"})"
                        + " { code name status } }")
                .execute()
                .path("createGrade.code").entity(String.class).isEqualTo("GRADE_ONE");

        graphQlTester.document("query { user(id:\"2001\") { id email status roleCodes } }")
                .execute()
                .path("user.id").entity(String.class).isEqualTo("2001");

        graphQlTester.document(
                        "query { schoolClass(gradeId:\"1001\",id:\"3001\") { id gradeCode status } }")
                .execute()
                .path("schoolClass.id").entity(String.class).isEqualTo("3001");
    }

    @Test
    void exposesStableErrorExtensions() {
        when(userManage.getUser(eq(new UserDetailQuery(9999L))))
                .thenThrow(new OrganizationApplicationException(
                        OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", "User not found"));

        graphQlTester.document("query { user(id:\"9999\") { id } }")
                .execute()
                .errors().satisfy(errors -> org.assertj.core.api.Assertions.assertThat(
                                errors.getFirst().getExtensions())
                        .containsKeys("code", "traceId", "timestamp", "fieldErrors"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
