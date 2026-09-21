package top.egon.cola.archetype.source.webopen.adapter.user.graphql;

import top.egon.cola.archetype.source.webopen.application.user.pojo.command.AssignRoleCommand;
import top.egon.cola.archetype.source.webopen.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.webopen.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.webopen.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.webopen.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.webopen.application.user.manage.UserManage;
import top.egon.cola.archetype.source.webopen.application.user.pojo.query.PermissionTreeQuery;
import top.egon.cola.archetype.source.webopen.application.user.pojo.query.UserDetailQuery;
import top.egon.cola.archetype.source.webopen.application.user.pojo.result.PermissionTreeResult;
import top.egon.cola.archetype.source.webopen.application.user.pojo.result.UserDetailResult;
import top.egon.cola.archetype.source.webopen.adapter.facade.impl.OrganizationFacadeSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;

@Controller("userResolver")
@RequiredArgsConstructor
public class UserResolver {

    @Qualifier("userManage")
    private final UserManage userManage;
    @Qualifier("roleManage")
    private final RoleManage roleManage;
    @Qualifier("permissionManage")
    private final PermissionManage permissionManage;

    @QueryMapping
    public UserDetailResult user(@Argument String id) {
        return userManage.getUser(new UserDetailQuery(
                OrganizationFacadeSupport.positiveId(id, "userId")));
    }

    @QueryMapping
    public PermissionTreeResult permissionTree(@Argument String userId) {
        return permissionManage.getPermissionTree(new PermissionTreeQuery(
                OrganizationFacadeSupport.positiveId(userId, "userId")));
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
        roleManage.assignRole(new AssignRoleCommand(requestId(key),
                OrganizationFacadeSupport.positiveId(input.userId(), "userId"), input.roleCode()));
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
