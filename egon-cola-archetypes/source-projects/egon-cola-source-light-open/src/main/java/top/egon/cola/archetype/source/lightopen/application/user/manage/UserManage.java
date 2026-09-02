package top.egon.cola.archetype.source.lightopen.application.user.manage;

import top.egon.cola.archetype.source.lightopen.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.lightopen.application.user.query.GetUserQuery;
import top.egon.cola.archetype.source.lightopen.application.user.result.UserResult;

public interface UserManage {
    UserResult create(CreateUserCommand command);

    UserResult get(GetUserQuery query);
}
