package top.egon.cola.archetype.source.lightopen.application.user.assemblers;

import top.egon.cola.archetype.source.lightopen.application.user.result.UserResult;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.UserSnapshot;
import org.springframework.stereotype.Component;

@Component
public class UserAssembler {
    public UserResult assemble(UserSnapshot user) {
        return new UserResult(user.id(), user.name(), user.email(), user.status().name());
    }
}
