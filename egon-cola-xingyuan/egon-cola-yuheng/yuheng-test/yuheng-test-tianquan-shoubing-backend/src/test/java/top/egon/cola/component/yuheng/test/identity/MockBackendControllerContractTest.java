package top.egon.cola.component.yuheng.test.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RequiresPermission;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class MockBackendControllerContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void declaresReadAndAdminPermissionsAtTheDownstreamBoundary()
            throws NoSuchMethodException {
        assertThat(MockBackendController.class.getAnnotation(
                EgonApiCatalog.class).interfaceGroupCode())
                .isEqualTo("identity");
        assertThat(MockBackendController.class.getMethod(
                "read", IdentityPrincipal.class).getAnnotation(
                        Operation.class).operationId())
                .isEqualTo("identity.read");
        assertThat(MockBackendController.class.getMethod(
                "read", IdentityPrincipal.class).getAnnotation(
                        EgonGatewayPolicy.class).exposure())
                .isEqualTo(EgonGatewayPolicy.Exposure.EXTERNAL);
        assertThat(MockBackendController.class.getMethod("read", IdentityPrincipal.class).getAnnotation(
                RequiresPermission.class).value()).isEqualTo("mock:read");
        assertThat(MockBackendController.class.getMethod("admin", IdentityPrincipal.class).getAnnotation(
                RequiresPermission.class).value()).isEqualTo("mock:admin");
    }

    @Test
    void keepsTheIdentityOpenApiGoldenProjectionStable() throws Exception {
        JsonNode golden;
        try (InputStream stream = getClass().getResourceAsStream(
                "/openapi/identity-golden.json")) {
            assertThat(stream).isNotNull();
            golden = objectMapper.readTree(stream);
        }
        assertThat(golden.path("group").asText()).isEqualTo("identity");
        assertThat(golden.path("operationIds").toString())
                .isEqualTo("[\"identity.admin\",\"identity.read\"]");
        assertThat(golden.path("paths").toString())
                .isEqualTo("[\"/api/mock/admin\",\"/api/mock/read\"]");
    }
}
