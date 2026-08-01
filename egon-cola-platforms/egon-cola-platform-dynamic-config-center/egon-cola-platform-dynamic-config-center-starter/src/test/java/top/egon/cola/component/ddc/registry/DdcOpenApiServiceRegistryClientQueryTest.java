package top.egon.cola.component.ddc.registry;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcOpenApiServiceRegistryClientQueryTest {

    @Test
    void instanceQueryIncludesTheCompleteBizApplicationScope() {
        var query = DdcOpenApiServiceRegistryClient.serviceKeyQuery(
                new DdcServiceKey(
                        "retail",
                        "orders",
                        "local",
                        "default",
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
                .containsEntry("group", List.of("default"))
                .containsEntry("version", List.of("1.0.0"));
    }

    @Test
    void serviceCatalogQueryIncludesTheCompleteBizApplicationScope() {
        var query = DdcOpenApiServiceRegistryClient.serviceQuery(
                new DdcServiceQuery(
                        "retail",
                        "orders",
                        "local",
                        "default",
                        DdcServiceKind.HTTP_PROVIDER,
                        "http",
                        null,
                        null,
                        null
                )
        );

        assertThat(query)
                .containsEntry("bizCode", List.of("retail"))
                .containsEntry("appCode", List.of("orders"));
    }
}
