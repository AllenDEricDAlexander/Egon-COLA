package top.egon.cola.platform.rbac3.core.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Rbac3RuntimeKeyFactoryTest {

    @Test
    void alwaysUsesTheTenantClusterHashTag() {
        Rbac3RuntimeKeyFactory keys = new Rbac3RuntimeKeyFactory();
        assertEquals("rbac3:{42}:session:99", keys.session("42", "99"));
        assertEquals("rbac3:{42}:auth-version:7", keys.authVersion("42", "7"));
        assertEquals("rbac3:{42}:policy-version", keys.policyVersion("42"));
        assertEquals("rbac3:{42}:snapshot:99:3", keys.snapshot("42", "99", 3));
        assertEquals("rbac3:{42}:fence:session:99", keys.sessionFence("42", "99"));
        assertEquals("rbac3:{42}:operation-mapping:finance:5", keys.operationMapping("42", "finance", 5));
        assertEquals("rbac3:{42}:operation-mapping:def-7:operation-9:5",
                keys.operationMapping("42", "def-7", "operation-9", 5));
        assertEquals("rbac3:{42}:key-ring", keys.keyRing("42"));
    }

    @Test
    void rejectsIdsThatCouldEscapeTheKeyGrammar() {
        Rbac3RuntimeKeyFactory keys = new Rbac3RuntimeKeyFactory();
        assertThrows(IllegalArgumentException.class,
                () -> keys.session("{other}", "99"));
        assertThrows(IllegalArgumentException.class,
                () -> keys.session("42", "99:other"));
    }
}
