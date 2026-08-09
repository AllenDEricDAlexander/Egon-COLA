package top.egon.cola.component.ddc.admin.security.rpc;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScope;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetPublishTaskRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServicesRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceQuery;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcRpcHmacScopeTest {

    private final DdcRpcScopeExtractor extractor =
            new DdcRpcScopeExtractor();

    @Test
    void extractsRequiredOptionalAndUnscopedRequestShapes() {
        DdcRpcScopeExtractor.Scope config = extractor.extract(
                "egon.ddc.v1.DdcConfigRuntimeService/PullConfig",
                PullConfigRequest.newBuilder()
                        .setScope(scope("biz-a", "dev", "app-a"))
                        .build()
        );
        assertThat(config).isEqualTo(new DdcRpcScopeExtractor.Scope(
                "SDK", "app-a", "dev", "biz-a"));

        DdcRpcScopeExtractor.Scope registry = extractor.extract(
                "egon.ddc.v1.DdcServiceRegistryService/GetServices",
                GetServicesRequest.newBuilder()
                        .setQuery(DdcServiceQuery.newBuilder()
                                .setBizCode("biz-a")
                                .setEnv("dev"))
                        .build()
        );
        assertThat(registry).isEqualTo(new DdcRpcScopeExtractor.Scope(
                "REGISTRY", null, "dev", "biz-a"));

        DdcRpcScopeExtractor.Scope task = extractor.extract(
                "egon.ddc.v1.DdcManagementService/GetPublishTask",
                GetPublishTaskRequest.newBuilder()
                        .setChangeId("change-1")
                        .build()
        );
        assertThat(task).isEqualTo(new DdcRpcScopeExtractor.Scope(
                "MANAGEMENT", null, null, null));
    }

    @Test
    void rejectsMissingRequiredScopeAndUnknownMethods() {
        assertThatThrownBy(() -> extractor.extract(
                "egon.ddc.v1.DdcConfigRuntimeService/PullConfig",
                PullConfigRequest.getDefaultInstance()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scope");
        assertThatThrownBy(() -> extractor.extract(
                "egon.ddc.v1.Unknown/Call",
                PullConfigRequest.getDefaultInstance()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown DDC RPC method");
    }

    @Test
    void servicePrincipalNeverTrustsRequestedOperatorIdentity() {
        DdcServicePrincipal principal = new DdcServicePrincipal(
                "management-a",
                "MANAGEMENT",
                Set.of("app-a"),
                Set.of("dev"),
                Set.of("biz-a"),
                Set.of("MANAGEMENT_CONFIG_WRITE"),
                "app-a",
                "dev",
                "biz-a"
        );

        assertThat(principal.auditOperator("claimed-user"))
                .isEqualTo("service:management-a [requested=claimed-user]");
        assertThat(principal.auditOperator(null))
                .isEqualTo("service:management-a");
        assertThat(principal.toString()).doesNotContain("secret");

        DdcHmacCredential credential = new DdcHmacCredential(
                "management-a", "access-a", "credential-secret",
                "MANAGEMENT", Set.of("*"), Set.of("*"), Set.of("*"),
                Set.of("*"));
        assertThat(credential.toString())
                .contains("secret=<redacted>")
                .doesNotContain("credential-secret");
    }

    private DdcScope scope(String bizCode, String env, String appCode) {
        return DdcScope.newBuilder()
                .setBizCode(bizCode)
                .setEnv(env)
                .setAppCode(appCode)
                .build();
    }
}
