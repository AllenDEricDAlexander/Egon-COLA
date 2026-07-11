package ${package}.domain.user;

import ${package}.domain.entities.user.User;
import ${package}.domain.enums.user.UserStatus;
import ${package}.domain.service.user.UserDomainService;
import ${package}.domain.service.user.impl.UserDomainServiceImpl;
import ${package}.domain.vos.user.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserDomainServiceTest {

    private final UserDomainService service = new UserDomainServiceImpl();

    @Test
    void createsNormalizedActiveUser() {
        User user = service.create(new UserId("u-1"), " Mario ", "MARIO@EXAMPLE.COM");

        assertEquals("Mario", user.name());
        assertEquals("mario@example.com", user.email());
        assertEquals(UserStatus.ACTIVE, user.status());
    }
}
