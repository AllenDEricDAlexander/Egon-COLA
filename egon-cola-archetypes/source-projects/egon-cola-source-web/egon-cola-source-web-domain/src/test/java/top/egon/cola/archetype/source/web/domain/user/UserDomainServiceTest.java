package top.egon.cola.archetype.source.web.domain.user;

import top.egon.cola.archetype.source.web.domain.user.entities.User;
import top.egon.cola.archetype.source.web.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;
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
