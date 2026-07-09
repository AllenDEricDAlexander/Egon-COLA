package ${package}.adapter.user.graphql;

import ${package}.application.user.manage.UserManage;
import ${package}.application.user.result.UserResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@GraphQlTest(UserResolver.class)
@ContextConfiguration(classes = UserResolver.class)
class UserResolverTest {
    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private UserManage userManage;

    @Test
    void resolves_user_through_graphql_boundary() {
        when(userManage.get(any()))
                .thenReturn(new UserResult("user-1", "Mario", "mario@example.com", "ACTIVE"));

        graphQlTester.document("{ user(id: \"user-1\") { id name email status } }")
                .execute()
                .path("user.name").entity(String.class).isEqualTo("Mario");
    }
}
