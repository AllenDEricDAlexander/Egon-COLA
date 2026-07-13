package ${package}.domain.user;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.service.impl.UserDomainServiceImpl;
import ${package}.domain.user.vos.UserId;
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
