package ${package}.infrastructure.user.service.impl;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.vos.UserId;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("userDomainService")
public class UserDomainServiceImpl implements UserDomainService {
    @Override
    public User createUser(String externalId, String name, String email) {
        return new User(
                new UserId(nextId()), externalId, name, email, UserStatus.ACTIVE);
    }

    private String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
