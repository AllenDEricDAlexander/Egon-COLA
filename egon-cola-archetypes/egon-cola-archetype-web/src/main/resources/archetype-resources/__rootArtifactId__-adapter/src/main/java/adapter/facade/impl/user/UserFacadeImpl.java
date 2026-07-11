package ${package}.adapter.facade.impl.user;

import ${package}.adapter.converter.UserAdapterConverter;
import ${package}.application.command.user.CreateUserCommand;
import ${package}.application.context.OrganizationRequestContext;
import ${package}.application.context.OrganizationRequestContextHolder;
import ${package}.application.exceptions.OrganizationApplicationException;
import ${package}.application.manage.user.UserManage;
import ${package}.application.query.user.UserDetailQuery;
import ${package}.facade.dto.user.CreateUserDTO;
import ${package}.facade.dto.user.UserDetailDTO;
import ${package}.facade.exceptions.OrganizationFacadeException;
import ${package}.facade.user.UserFacade;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Component("userFacade")
@Validated
public class UserFacadeImpl implements UserFacade {

    private final UserManage userManage;
    private final UserAdapterConverter converter;

    public UserFacadeImpl(UserManage userManage, UserAdapterConverter converter) {
        this.userManage = userManage;
        this.converter = converter;
    }

    @Override
    public UserDetailDTO createUser(CreateUserDTO request) {
        return withSystemContext(() -> converter.toDTO(userManage.createUser(
            new CreateUserCommand(UUID.randomUUID().toString(), request.name(), request.email()))));
    }

    @Override
    public UserDetailDTO getUser(String userId) {
        return withSystemContext(() -> converter.toDTO(userManage.getUser(new UserDetailQuery(userId))));
    }

    private <T> T withSystemContext(Supplier<T> action) {
        boolean created = OrganizationRequestContextHolder.current().isEmpty();
        if (created) {
            OrganizationRequestContextHolder.set(new OrganizationRequestContext(
                "facade-system", Set.of("SYSTEM"), UUID.randomUUID().toString()));
        }
        try {
            return action.get();
        } catch (OrganizationApplicationException failure) {
            String traceId = OrganizationRequestContextHolder.current()
                .map(OrganizationRequestContext::traceId).orElse("unknown");
            throw new OrganizationFacadeException(failure.code(), failure.getMessage(), traceId);
        } finally {
            if (created) {
                OrganizationRequestContextHolder.clear();
            }
        }
    }
}
