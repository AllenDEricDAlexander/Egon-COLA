package ${package}.adapter.user.graphql;

import ${package}.adapter.user.convertor.UserAdapterConvertor;
import ${package}.adapter.user.vo.PermissionTreeVO;
import ${package}.adapter.user.vo.UserDetailVO;
import ${package}.application.user.manage.UserManage;
import ${package}.application.user.manage.PermissionManage;
import ${package}.application.user.query.GetUserQuery;
import ${package}.application.user.query.GetUserPermissionsQuery;
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
        return convertor.toUserDetail(userManage.get(new GetUserQuery(id)));
    }

    @QueryMapping
    public List<PermissionTreeVO> permissions(@Argument String userId) {
        return convertor.toPermissionTree(
                permissionManage.getByUser(new GetUserPermissionsQuery(userId)));
    }
}
