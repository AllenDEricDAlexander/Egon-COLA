package top.egon.cola.archetype.source.web.adapter.user.graphql;

import lombok.extern.slf4j.Slf4j;
import java.util.Set;
import top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties;
import top.egon.cola.platform.tianquan.jianshen.starter.field.FieldWriteAuthorizationCommand;
import top.egon.cola.platform.tianquan.jianshen.starter.field.Rbac3FieldWriteAuthorizationComponent;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RBACAPIResource;
import org.springframework.security.access.prepost.PreAuthorize;
import top.egon.cola.archetype.source.web.application.user.pojo.command.AssignRoleCommand;
import top.egon.cola.archetype.source.web.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.web.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.web.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.web.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
import top.egon.cola.archetype.source.web.application.user.pojo.query.PermissionTreeQuery;
import top.egon.cola.archetype.source.web.application.user.pojo.query.UserDetailQuery;
import top.egon.cola.archetype.source.web.application.user.pojo.result.PermissionTreeResult;
import top.egon.cola.archetype.source.web.application.user.pojo.result.UserDetailResult;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
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
@Slf4j
public class UserResolver {
    @Qualifier("rbac3FieldWriteAuthorizationComponent")
    private final Rbac3FieldWriteAuthorizationComponent fieldWrite;
    @Qualifier("egon.cola.platform.tianquan.jianshen-top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties")
    private final Rbac3StarterProperties authorizationProperties;


    @Qualifier("userManage")
    private final UserManage userManage;
    @Qualifier("roleManage")
    private final RoleManage roleManage;
    @Qualifier("permissionManage")
    private final PermissionManage permissionManage;

    @QueryMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:user:read')")
    @RBACAPIResource(code = "egon-cola-source-web.graphql.query.user", permission = "egon-cola-source-web:user:read", name = "User")
    public UserDetailResult user(@Argument String id) {
        return userManage.getUser(new UserDetailQuery(
                OrganizationFacadeSupport.positiveId(id, "userId")));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:permission:read')")
    @RBACAPIResource(code = "egon-cola-source-web.graphql.query.permissionTree", permission = "egon-cola-source-web:permission:read", name = "Permission tree")
    public PermissionTreeResult permissionTree(@Argument String userId) {
        return permissionManage.getPermissionTree(new PermissionTreeQuery(
                OrganizationFacadeSupport.positiveId(userId, "userId")));
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:user:create')")
    @RBACAPIResource(code = "egon-cola-source-web.graphql.mutation.createUser", permission = "egon-cola-source-web:user:create", name = "Create user")
    public UserDetailResult createUser(
            @Argument CreateUserInput input,
            @ContextValue(name = "idempotencyKey", required = false) String key) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-web:user:create", authorizationProperties.getSystemCode(),
                "egon-cola-source-web.graphql.mutation.createUser", Set.of("name","email")));
        return userManage.createUser(new CreateUserCommand(requestId(key), input.name(), input.email()));
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:role:assign')")
    @RBACAPIResource(code = "egon-cola-source-web.graphql.mutation.assignRole", permission = "egon-cola-source-web:role:assign", name = "Assign role")
    public boolean assignRole(
            @Argument AssignRoleInput input,
            @ContextValue(name = "idempotencyKey", required = false) String key) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-web:role:assign", authorizationProperties.getSystemCode(),
                "egon-cola-source-web.graphql.mutation.assignRole", Set.of("roleCode")));
        roleManage.assignRole(new AssignRoleCommand(requestId(key),
                OrganizationFacadeSupport.positiveId(input.userId(), "userId"), input.roleCode()));
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-web:permission:grant')")
    @RBACAPIResource(code = "egon-cola-source-web.graphql.mutation.grantPermission", permission = "egon-cola-source-web:permission:grant", name = "Grant permission")
    public boolean grantPermission(
            @Argument GrantPermissionInput input,
            @ContextValue(name = "idempotencyKey", required = false) String key) {
        fieldWrite.requireWritableFields(new FieldWriteAuthorizationCommand("egon-cola-source-web:permission:grant", authorizationProperties.getSystemCode(),
                "egon-cola-source-web.graphql.mutation.grantPermission", Set.of("permissionCode")));
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
