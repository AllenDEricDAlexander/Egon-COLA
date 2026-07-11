package ${package}.adapter.graphql;

import ${package}.application.command.user.AssignRoleCommand;
import ${package}.application.command.user.CreateUserCommand;
import ${package}.application.command.user.GrantPermissionCommand;
import ${package}.application.manage.user.PermissionManage;
import ${package}.application.manage.user.RoleManage;
import ${package}.application.manage.user.UserManage;
import ${package}.application.query.user.PermissionTreeQuery;
import ${package}.application.query.user.UserDetailQuery;
import ${package}.application.result.user.PermissionTreeResult;
import ${package}.application.result.user.UserDetailResult;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class UserResolver {

    private final UserManage userManage;
    private final RoleManage roleManage;
    private final PermissionManage permissionManage;

    public UserResolver(UserManage userManage, RoleManage roleManage, PermissionManage permissionManage) {
        this.userManage = userManage;
        this.roleManage = roleManage;
        this.permissionManage = permissionManage;
    }

    @QueryMapping
    public UserDetailResult user(@Argument String id) {
        return userManage.getUser(new UserDetailQuery(id));
    }

    @QueryMapping
    public PermissionTreeResult permissionTree(@Argument String userId) {
        return permissionManage.getPermissionTree(new PermissionTreeQuery(userId));
    }

    @MutationMapping
    public UserDetailResult createUser(
            @Argument CreateUserInput input,
            @ContextValue(name = "idempotencyKey", required = false) String key) {
        return userManage.createUser(new CreateUserCommand(requestId(key), input.name(), input.email()));
    }

    @MutationMapping
    public boolean assignRole(
            @Argument AssignRoleInput input,
            @ContextValue(name = "idempotencyKey", required = false) String key) {
        roleManage.assignRole(new AssignRoleCommand(requestId(key), input.userId(), input.roleCode()));
        return true;
    }

    @MutationMapping
    public boolean grantPermission(
            @Argument GrantPermissionInput input,
            @ContextValue(name = "idempotencyKey", required = false) String key) {
        permissionManage.grantPermission(
                new GrantPermissionCommand(requestId(key), input.roleCode(), input.permissionCode()));
        return true;
    }

    private static String requestId(String key) {
        return key == null || key.isBlank() ? UUID.randomUUID().toString() : key;
    }

    public record CreateUserInput(String name, String email) {}
    public record AssignRoleInput(String userId, String roleCode) {}
    public record GrantPermissionInput(String roleCode, String permissionCode) {}
}
