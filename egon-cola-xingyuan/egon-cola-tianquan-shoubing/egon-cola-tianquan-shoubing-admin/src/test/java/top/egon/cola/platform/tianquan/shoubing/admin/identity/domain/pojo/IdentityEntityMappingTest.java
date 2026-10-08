package top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.converter.IdentityCredentialConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.converter.IdentityUserConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.po.IdentityCredentialPO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.po.IdentityUserPO;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUser;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUserStatus;
import top.egon.cola.platform.tianquan.shoubing.core.identity.PasswordCredential;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityEntityMappingTest {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");
    private final IdentityUserConverter users = Mappers.getMapper(IdentityUserConverter.class);
    private final IdentityCredentialConverter credentials = Mappers.getMapper(IdentityCredentialConverter.class);

    @Test
    void userPersistenceMappingRetainsExternalSubjectAndSecurityState() {
        IdentityUser user = new IdentityUser(
                "1001", "Alice", "alice", "Alice A", IdentityUserStatus.LOCKED,
                3, NOW.plusSeconds(300), NOW.minusSeconds(60), 9L
        );

        IdentityUserPO po = users.toSource(user);

        assertNull(po.getId());
        assertEquals("1001", po.getIdentitySub());
        assertEquals(user, users.toTarget(po));
    }

    @Test
    void credentialPersistenceMappingRetainsHashFlagsAndVersionWithoutExposingHash() throws Exception {
        PasswordCredential credential = new PasswordCredential(
                "1001", "{argon2}encoded", NOW, true,
                PasswordCredential.Status.ACTIVE, 4L
        );

        IdentityCredentialPO po = credentials.toSource(credential);

        assertNull(po.getId());
        assertEquals("1001", po.getIdentitySub());
        assertEquals("{argon2}encoded", po.getPasswordHash());
        assertEquals(credential, credentials.toTarget(po));
        assertFalse(po.toString().contains("{argon2}encoded"));
        assertFalse(po.toString().contains("1001"));
        assertFalse(new ObjectMapper().findAndRegisterModules()
                .writeValueAsString(po)
                .contains("{argon2}encoded"));
        assertFalse(new ObjectMapper().findAndRegisterModules()
                .writeValueAsString(po)
                .contains("1001"));
        assertTrue(po.getMustChangePassword());
    }
}
