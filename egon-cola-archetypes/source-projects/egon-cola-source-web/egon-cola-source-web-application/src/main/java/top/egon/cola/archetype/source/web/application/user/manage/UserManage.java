package top.egon.cola.archetype.source.web.application.user.manage;

import top.egon.cola.archetype.source.web.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.web.application.user.query.UserDetailQuery;
import top.egon.cola.archetype.source.web.application.user.result.UserDetailResult;

public interface UserManage {

    UserDetailResult createUser(CreateUserCommand command);

    UserDetailResult getUser(UserDetailQuery query);
}
