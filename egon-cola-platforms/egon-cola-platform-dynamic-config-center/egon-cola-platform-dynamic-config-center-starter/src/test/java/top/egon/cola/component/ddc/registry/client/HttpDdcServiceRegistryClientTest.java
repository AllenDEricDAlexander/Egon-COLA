package top.egon.cola.component.ddc.registry.client;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpDdcServiceRegistryClientTest {

    @Test
    void instanceQueryIncludesTheCompleteBizApplicationScope() {
        var query = HttpDdcServiceRegistryClient.serviceKeyQuery(
                new DdcServiceKey(
                        "retail",
                        "local",
                        "orders",
                        DdcServiceKind.HTTP_PROVIDER,
                        "orders-http",
                        null,
                        null,
                        "http"
                )
        );

        assertThat(query)
                .containsEntry("bizCode", List.of("retail"))
                .containsEntry("appCode", List.of("orders"))
                .doesNotContainKey("namespace")
                .containsEntry("group", List.of("default"))
                .containsEntry("version", List.of("1.0.0"));
    }

    @Test
    void serviceCatalogQueryOnlyIncludesProvidedFilters() {
        var query = HttpDdcServiceRegistryClient.serviceQuery(
                new DdcServiceQuery(
                        "retail",
                        "local",
                        null,
                        DdcServiceKind.HTTP_PROVIDER,
                        "http",
                        null,
                        null,
                        null
                )
        );

        assertThat(query)
                .containsEntry("bizCode", List.of("retail"))
                .containsEntry("env", List.of("local"))
                .containsEntry("serviceKind", List.of("HTTP_PROVIDER"))
                .doesNotContainKeys("appCode", "namespace", "serviceName", "group", "version");
    }
}
