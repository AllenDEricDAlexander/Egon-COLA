package top.egon.cola.archetype.source.webopen.adapter.user.facade.impl;

import top.egon.cola.archetype.source.webopen.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.webopen.application.user.manage.UserManage;
import top.egon.cola.archetype.source.webopen.application.user.query.UserDetailQuery;
import top.egon.cola.archetype.source.webopen.application.user.result.UserDetailResult;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.CreateUserRequest;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.DubboUserServiceTriple;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.GetUserRequest;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.User;
import top.egon.cola.archetype.source.webopen.adapter.facade.impl.OrganizationFacadeSupport;
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
