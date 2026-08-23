package ${package}.application.user.assemblers;

import ${package}.application.user.result.UserResult;
import ${package}.domain.user.vos.UserSnapshot;
import org.springframework.stereotype.Component;

@Component
public class UserAssembler {
    public UserResult assemble(UserSnapshot user) {
        return new UserResult(user.id(), user.name(), user.email(), user.status().name());
    }
}
