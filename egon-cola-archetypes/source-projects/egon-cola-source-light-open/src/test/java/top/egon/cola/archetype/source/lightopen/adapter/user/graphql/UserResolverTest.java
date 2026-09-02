package top.egon.cola.archetype.source.lightopen.adapter.user.graphql;

import top.egon.cola.archetype.source.lightopen.adapter.user.convertor.UserAdapterConvertorImpl;
import top.egon.cola.archetype.source.lightopen.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.lightopen.application.user.manage.UserManage;
import top.egon.cola.archetype.source.lightopen.application.user.result.PermissionDetailResult;
import top.egon.cola.archetype.source.lightopen.application.user.result.UserResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@GraphQlTest(UserResolver.class)
@ContextConfiguration(classes = {UserResolver.class, UserAdapterConvertorImpl.class})
class UserResolverTest {
    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private UserManage userManage;

    @MockitoBean
    private PermissionManage permissionManage;

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
