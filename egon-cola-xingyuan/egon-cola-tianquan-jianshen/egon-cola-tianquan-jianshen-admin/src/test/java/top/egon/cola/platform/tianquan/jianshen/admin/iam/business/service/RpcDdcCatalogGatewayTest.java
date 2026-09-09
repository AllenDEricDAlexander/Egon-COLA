package top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.api.client.DdcManagementClient;
import top.egon.cola.component.tianshu.model.management.DdcManagementApp;
import top.egon.cola.component.tianshu.model.management.DdcManagementBiz;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RpcDdcCatalogGatewayTest {

    @Test
    void mapsApplicationWithParentBusinessFacts() {
        DdcManagementClient client = mock(DdcManagementClient.class);
        when(client.getApp("tianshu-app-1")).thenReturn(Optional.of(
                new DdcManagementApp(
                        "tianshu-app-1", "tianshu-biz-1", "orders",
                        "console", "Console", true, true)));

        RpcDdcCatalogGateway gateway = new RpcDdcCatalogGateway(client);

        assertThat(gateway.findApplication("tianshu-app-1"))
                .contains(new ApplicationCatalogEntry(
                        "tianshu-app-1", "tianshu-biz-1", "orders",
                        "console", "Console", true, true));
    }

    @Test
    void mapsBusinessCatalogAndDelegatesReadOnlyQuery() {
        DdcManagementClient client = mock(DdcManagementClient.class);
        when(client.listBizs(any())).thenReturn(List.of(
                new DdcManagementBiz("tianshu-biz-1", "orders", "Orders", true)));

        RpcDdcCatalogGateway gateway = new RpcDdcCatalogGateway(client);

        assertThat(gateway.listBusinesses("order"))
                .containsExactly(new BusinessCatalogEntry(
                        "tianshu-biz-1", "orders", "Orders", true));
        verify(client).listBizs(
                new top.egon.cola.component.tianshu.model.management.DdcManagementBizQuery(
                        "order", null));
    }

    @Test
    void preservesAbsentApplicationAsAnEmptyOptional() {
        DdcManagementClient client = mock(DdcManagementClient.class);
        when(client.getApp("missing")).thenReturn(Optional.empty());

        assertThat(new RpcDdcCatalogGateway(client)
                .findApplication("missing")).isEmpty();
    }
}
