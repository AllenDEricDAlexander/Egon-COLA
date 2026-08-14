package top.egon.cola.platform.rbac3.admin.identity;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.identity.domain.dto.IdentityMembershipResolveRequestDTO;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityMembershipResolveRequestDTOTest {

    @Test
    void namesTheInternalRequestByItsMembershipResponsibility() {
        var request = new IdentityMembershipResolveRequestDTO("identity-1", "tenant-1");

        assertThat(request.identitySub()).isEqualTo("identity-1");
        assertThat(request.tenantId()).isEqualTo("tenant-1");
    }
}
