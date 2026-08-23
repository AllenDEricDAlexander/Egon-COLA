package ${package}.application.user.manage;

import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.query.UserDetailQuery;
import ${package}.application.user.result.UserDetailResult;

public interface UserManage {

    UserDetailResult createUser(CreateUserCommand command);

    UserDetailResult getUser(UserDetailQuery query);
}
