#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.user.facade.impl;

import ${package}.application.user.command.CreateUserCommand;
import ${package}.application.user.manage.UserManage;
import ${package}.application.user.query.UserDetailQuery;
import ${package}.application.user.result.UserDetailResult;
import ${package}.facade.organization.v1.CreateUserRequest;
import ${package}.facade.organization.v1.DubboUserServiceTriple;
import ${package}.facade.organization.v1.GetUserRequest;
import ${package}.facade.organization.v1.User;
import ${package}.adapter.facade.impl.OrganizationFacadeSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserFacadeImpl extends DubboUserServiceTriple.UserServiceImplBase {

    private final UserManage userManage;
    private final OrganizationFacadeSupport support;

    @Override
    public User createUser(CreateUserRequest request) {
        return support.invoke(() -> {
            require(request, "createUser request");
            UserDetailResult result = userManage.createUser(new CreateUserCommand(
                    support.requestId(), request.getName(), request.getEmail()));
            return toProto(result);
        });
    }

    @Override
    public User getUser(GetUserRequest request) {
        return support.invoke(() -> {
            require(request, "getUser request");
            UserDetailResult result = userManage.getUser(new UserDetailQuery(
                    OrganizationFacadeSupport.positiveId(request.getUserId(), "userId")));
            return toProto(result);
        });
    }

    private static void require(Object request, String name) {
        if (request == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    private static User toProto(UserDetailResult result) {
        return User.newBuilder()
                .setId(result.id())
                .setName(result.name())
                .setEmail(result.email())
                .setStatus(result.status())
                .addAllRoleCodes(result.roleCodes())
                .build();
    }
}
