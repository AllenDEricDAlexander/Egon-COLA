package ${package}.adapter;

import ${package}.adapter.graphql.SchoolClassResolver;
import ${package}.adapter.graphql.UserResolver;
import ${package}.adapter.handler.OrganizationGraphQlExceptionResolver;
import ${package}.application.manage.teaching.GradeManage;
import ${package}.application.manage.teaching.SchoolClassManage;
import ${package}.application.manage.user.PermissionManage;
import ${package}.application.manage.user.RoleManage;
import ${package}.application.manage.user.UserManage;
import ${package}.application.query.user.UserDetailQuery;
import ${package}.application.result.teaching.GradeDetailResult;
import ${package}.application.result.user.UserDetailResult;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.exceptions.OrganizationFailureType;
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

    @BeforeEach
    void setUp() {
        graphQlTester = ExecutionGraphQlServiceTester.create(executionGraphQlService);
    }

    @Test
    void exposesBothDomainQueriesAndMutations() {
        when(gradeManage.createGrade(any()))
                .thenReturn(new GradeDetailResult("g-1", "GRADE_ONE", "Grade One", "ACTIVE"));
        when(userManage.getUser(new UserDetailQuery("u-1")))
                .thenReturn(new UserDetailResult("u-1", "Mario", "mario@example.com", "ACTIVE", List.of()));

        graphQlTester.document("mutation { createGrade(input:{code:\"GRADE_ONE\",name:\"Grade One\"})"
                        + " { code name status } }")
                .execute()
                .path("createGrade.code").entity(String.class).isEqualTo("GRADE_ONE");

        graphQlTester.document("query { user(id:\"u-1\") { id email status roleCodes } }")
                .execute()
                .path("user.id").entity(String.class).isEqualTo("u-1");
    }

    @Test
    void exposesStableErrorExtensions() {
        when(userManage.getUser(eq(new UserDetailQuery("missing"))))
                .thenThrow(new OrganizationApplicationException(
                        OrganizationFailureType.NOT_FOUND, "ORG_NOT_FOUND", "User not found"));

        graphQlTester.document("query { user(id:\"missing\") { id } }")
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
