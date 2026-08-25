package ${package}.domain.user;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.vos.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserDomainServiceTest {
    @Test
    void keepsLongIdentityAndNormalizedUserValues() {
        User user = new User(new UserId(1001L), " Mario ", "MARIO@EXAMPLE.COM", UserStatus.ACTIVE);

        assertEquals(1001L, user.id().value());
        assertEquals("Mario", user.name());
        assertEquals("mario@example.com", user.email());
        assertEquals(UserStatus.ACTIVE, user.status());
    }
}
