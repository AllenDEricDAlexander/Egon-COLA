package top.egon.cola.archetype.source.lightopen.adapter.user.graphql;

import top.egon.cola.archetype.source.lightopen.adapter.user.convertor.UserAdapterConvertor;
import top.egon.cola.archetype.source.lightopen.adapter.user.vo.PermissionTreeVO;
import top.egon.cola.archetype.source.lightopen.adapter.user.vo.UserDetailVO;
import top.egon.cola.archetype.source.lightopen.application.user.manage.UserManage;
import top.egon.cola.archetype.source.lightopen.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.lightopen.application.user.query.GetUserQuery;
import top.egon.cola.archetype.source.lightopen.application.user.query.GetUserPermissionsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserResolver {
    private final UserManage userManage;
    private final PermissionManage permissionManage;
    private final UserAdapterConvertor convertor;

    @QueryMapping
    public UserDetailVO user(@Argument String id) {
        return convertor.toUserDetail(userManage.get(new GetUserQuery(Long.valueOf(id))));
    }

    @QueryMapping
    public List<PermissionTreeVO> permissions(@Argument String userId) {
        return convertor.toPermissionTree(
                permissionManage.getByUser(new GetUserPermissionsQuery(Long.valueOf(userId))));
    }
}
