package ${package}.adapter.user.convertor;

import ${package}.adapter.user.vo.UserDetailVO;
import ${package}.adapter.user.vo.PermissionTreeVO;
import ${package}.application.user.result.PermissionDetailResult;
import ${package}.application.user.result.UserResult;

import java.util.List;

public final class UserAdapterConvertor {
    private UserAdapterConvertor() {
    }

    public static UserDetailVO toUserDetail(UserResult result) {
        return new UserDetailVO(result.id(), result.name(), result.email(), result.status());
    }

    public static List<PermissionTreeVO> toPermissionTree(List<PermissionDetailResult> results) {
        return results.stream()
                .map(result -> new PermissionTreeVO(result.code(), result.name(), List.of()))
                .toList();
    }
}
