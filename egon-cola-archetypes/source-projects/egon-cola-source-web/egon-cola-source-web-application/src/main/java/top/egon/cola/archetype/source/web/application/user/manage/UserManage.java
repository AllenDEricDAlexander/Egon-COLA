package top.egon.cola.archetype.source.web.application.user.manage;

import top.egon.cola.archetype.source.web.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.web.application.user.pojo.query.UserDetailQuery;
import top.egon.cola.archetype.source.web.application.user.pojo.result.UserDetailResult;

public interface UserManage {

    UserDetailResult createUser(CreateUserCommand command);

    UserDetailResult getUser(UserDetailQuery query);
}
