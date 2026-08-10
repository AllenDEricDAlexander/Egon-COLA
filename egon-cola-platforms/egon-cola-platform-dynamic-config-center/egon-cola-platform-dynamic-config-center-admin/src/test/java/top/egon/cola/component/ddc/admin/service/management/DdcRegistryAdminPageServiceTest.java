package top.egon.cola.component.ddc.admin.service.management;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceKey;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRegistryAdminPageServiceTest {

    private final DdcManagementFacade facade = mock(DdcManagementFacade.class);

    private final DdcRegistryAdminPageService service =
            new DdcRegistryAdminPageService(facade);

    private final DdcManagementServiceQuery query =
            new DdcManagementServiceQuery(
                    "infra", null, "prod", "gateway",
                    null, null, null, null, null
            );

    @Test
    void pagesSortedServiceKeysWithoutChangingCatalog() {
        DdcManagementServiceKey a = service("gateway-a", "svc-a");
        DdcManagementServiceKey b = service("gateway-b", "svc-b");
        when(facade.getServiceKeys(query)).thenReturn(
                new DdcManagementServiceCatalog(
                        9L, Instant.EPOCH, List.of(b, a)
                )
        );

        Page<DdcManagementServiceKey> page = service.pageServices(
                query, new PageQuery(1, 1)
        );

        assertThat(page.getContent()).containsExactly(a);
        assertThat(page.getTotalElements()).isEqualTo(2);
        verify(facade).getServiceKeys(query);
    }

    @Test
    void pagesInstancesInStableStatusAndEndpointOrder() {
        DdcManagementServiceKey key = service("gateway", "svc");
        DdcManagementServiceInstance a = instance(
                "instance-a", "192.168.1.1", 19000, "ONLINE"
        );
        DdcManagementServiceInstance b = instance(
                "instance-b", "192.168.1.2", 19001, "ONLINE"
        );
        DdcManagementServiceInstance c = instance(
                "instance-c", "192.168.1.1", 19000, "SUSPECT"
        );
        when(facade.getInstances(query)).thenReturn(
                new DdcManagementServiceSnapshot(
                        key, 10L, Instant.EPOCH, List.of(c, b, a)
                )
        );

        Page<DdcManagementServiceInstance> page = service.pageInstances(
                query, new PageQuery(1, 2)
        );

        assertThat(page.getContent()).containsExactly(a, b);
        assertThat(page.getTotalElements()).isEqualTo(3);
        verify(facade).getInstances(query);
    }

    private DdcManagementServiceKey service(
            String appCode,
            String serviceId
    ) {
        return new DdcManagementServiceKey(
                "infra", "prod", appCode, serviceId,
                "RPC_PROVIDER", "EchoService", "default", "1.0.0", "grpc"
        );
    }

    private DdcManagementServiceInstance instance(
            String instanceId,
            String host,
            int port,
            String status
    ) {
        return new DdcManagementServiceInstance(
                instanceId, "lease-" + instanceId, host, port, false,
                Map.of(), status, Instant.EPOCH, Instant.EPOCH, Instant.EPOCH
        );
    }
}
