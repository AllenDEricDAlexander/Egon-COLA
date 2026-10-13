package top.egon.cola.archetype.source.light.adapter.user.graphql;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import top.egon.cola.archetype.source.light.adapter.user.pojo.convertor.UserAdapterConvertorImpl;
import top.egon.cola.archetype.source.light.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.light.application.user.manage.UserManage;
import top.egon.cola.archetype.source.light.application.user.pojo.result.PermissionDetailResult;
import top.egon.cola.archetype.source.light.application.user.pojo.result.UserResult;
import top.egon.cola.archetype.source.light.start.config.NativeSecurityContractTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@TestPropertySource(properties = "TIANQUAN_SHOUBING_RESOURCE_URI=https://light.example.test")
@Import(NativeSecurityContractTest.SecurityTestBeans.class)
@GraphQlTest(UserResolver.class)
@ContextConfiguration(classes = {UserResolver.class, UserAdapterConvertorImpl.class})
class UserResolverTest {
    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean(name = "userManageImpl")
    private UserManage userManage;

    @MockitoBean(name = "permissionManageImpl")
    private PermissionManage permissionManage;

    @Autowired private NativeSecurityContractTest.SecurityFixture security;
    @BeforeEach void verifiedContext() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(security.authenticatedUser("1001", Set.of("egon-cola-source-light:user:read", "egon-cola-source-light:permission:read")));
    }
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); MDC.clear(); }

    @Test
    void resolves_user_through_graphql_boundary() {
        when(userManage.get(any()))
                .thenReturn(new UserResult(1001L, "Mario", "mario@example.com", "ACTIVE"));

        graphQlTester.document("{ user(id: \"1001\") { id name email status } }")
                .execute()
                .path("user.name").entity(String.class).isEqualTo("Mario");
    }

    @Test
    void resolves_user_permissions_through_application_boundary() {
        when(permissionManage.getByUser(any())).thenReturn(List.of(
                new PermissionDetailResult("course:read", "Read courses")));

        graphQlTester.document("{ permissions(userId: \"1001\") { code name children { code } } }")
                .execute()
                .path("permissions[0].code").entity(String.class).isEqualTo("course:read")
                .path("permissions[0].children").entityList(Object.class).hasSize(0);
    }
}
