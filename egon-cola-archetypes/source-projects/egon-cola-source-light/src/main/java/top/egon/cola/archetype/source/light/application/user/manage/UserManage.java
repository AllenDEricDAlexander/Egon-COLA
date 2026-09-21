package top.egon.cola.archetype.source.light.application.user.manage;

import top.egon.cola.archetype.source.light.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.light.application.user.pojo.query.GetUserQuery;
import top.egon.cola.archetype.source.light.application.user.pojo.result.UserResult;

public interface UserManage {
    UserResult create(CreateUserCommand command);

    UserResult get(GetUserQuery query);
}
