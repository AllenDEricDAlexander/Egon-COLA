package top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IamTenantControllerTest {

    @Test
    void removesTenantCatalogController() {
        assertThatThrownBy(() -> Class.forName(
                "top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant.controller.TenantController"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
