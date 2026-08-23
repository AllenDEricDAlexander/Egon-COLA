package ${package}.infrastructure.user.service.impl;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.vos.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.egon.cola.component.common.id.generator.IdGenerator;

@Service("userDomainService")
@RequiredArgsConstructor
public class UserDomainServiceImpl implements UserDomainService {
    private final IdGenerator idGenerator;

    @Override
    public User createUser(String externalId, String name, String email) {
        return new User(
                new UserId(idGenerator.nextId()),
                externalId,
                name,
                email,
                UserStatus.ACTIVE);
    }
}
