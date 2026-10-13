package top.egon.cola.archetype.source.light.adapter.user.graphql;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import top.egon.cola.archetype.source.light.adapter.user.pojo.convertor.UserAdapterConvertor;
import top.egon.cola.archetype.source.light.adapter.user.pojo.vo.PermissionTreeVO;
import top.egon.cola.archetype.source.light.adapter.user.pojo.vo.UserDetailVO;
import top.egon.cola.archetype.source.light.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.light.application.user.manage.UserManage;
import top.egon.cola.archetype.source.light.application.user.pojo.query.GetUserPermissionsQuery;
import top.egon.cola.archetype.source.light.application.user.pojo.query.GetUserQuery;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RBACAPIResource;

@Controller("userResolver")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class UserResolver {
    @Qualifier("userManageImpl")
    private final UserManage userManage;
    @Qualifier("permissionManageImpl")
    private final PermissionManage permissionManage;
    @Qualifier("userAdapterConvertorImpl")
    private final UserAdapterConvertor convertor;

    @QueryMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-light:user:read')")
    @RBACAPIResource(code = "egon-cola-source-light.graphql.query.user", permission = "egon-cola-source-light:user:read", name = "Read user")
    public UserDetailVO user(@Argument String id) {
        return convertor.toTarget(userManage.get(new GetUserQuery(Long.valueOf(id))));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('SCOPE_egon-cola-source-light:permission:read')")
    @RBACAPIResource(code = "egon-cola-source-light.graphql.query.permissions", permission = "egon-cola-source-light:permission:read", name = "Read user permissions")
    public List<PermissionTreeVO> permissions(@Argument String userId) {
        return convertor.toPermissionTree(
                permissionManage.getByUser(new GetUserPermissionsQuery(Long.valueOf(userId))));
    }
}
