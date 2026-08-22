package top.egon.cola.platform.rbac3.admin.iam.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityMembershipResolveRequestDTOTest {

    @Test
    void removesTheInternalMembershipResolveRequestType() {
        assertThatThrownBy(() -> Class.forName(
                "top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.IdentityMembershipResolveRequestDTO"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
