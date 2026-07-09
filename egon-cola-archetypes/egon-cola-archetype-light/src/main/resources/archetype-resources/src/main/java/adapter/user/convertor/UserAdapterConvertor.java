package ${package}.adapter.user.convertor;

import ${package}.adapter.user.vo.UserDetailVO;
import ${package}.application.user.result.UserResult;

public final class UserAdapterConvertor {
    private UserAdapterConvertor() {
    }

    public static UserDetailVO toUserDetail(UserResult result) {
        return new UserDetailVO(result.id(), result.name(), result.email(), result.status());
    }
}
