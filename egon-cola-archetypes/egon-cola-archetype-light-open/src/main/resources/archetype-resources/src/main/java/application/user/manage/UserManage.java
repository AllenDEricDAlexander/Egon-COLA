package ${package}.application.user.manage;

import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.query.GetUserQuery;
import ${package}.application.user.result.UserResult;

public interface UserManage {
    UserResult create(CreateUserCommand command);

    UserResult get(GetUserQuery query);
}
