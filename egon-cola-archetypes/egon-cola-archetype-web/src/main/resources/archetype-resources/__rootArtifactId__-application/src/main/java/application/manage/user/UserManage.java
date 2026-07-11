package ${package}.application.manage.user;

import ${package}.application.command.user.CreateUserCommand;
import ${package}.application.query.user.UserDetailQuery;
import ${package}.application.result.user.UserDetailResult;

public interface UserManage {

    UserDetailResult createUser(CreateUserCommand command);

    UserDetailResult getUser(UserDetailQuery query);
}
