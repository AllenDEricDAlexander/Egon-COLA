package top.egon.cola.component.ddc.admin.service.registry;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRegistryFacadeTest {

    @Test
    void delegatesAllRegistryOperationsWithoutChangingResults() {
        DdcServiceRegistryService service = mock(DdcServiceRegistryService.class);
        DdcRegistryFacade facade = new DdcRegistryFacade(service);
        DdcServiceRegistration registration = mock(DdcServiceRegistration.class);
        DdcServiceLeaseRequest lease = mock(DdcServiceLeaseRequest.class);
        DdcServiceKey key = mock(DdcServiceKey.class);
        DdcServiceQuery query = mock(DdcServiceQuery.class);
        DdcLeaseSession session = mock(DdcLeaseSession.class);
        DdcLeaseOperationResult renewed = mock(DdcLeaseOperationResult.class);
        DdcLeaseOperationResult deleted = mock(DdcLeaseOperationResult.class);
        DdcServiceSnapshot snapshot = mock(DdcServiceSnapshot.class);
        DdcServiceCatalogSnapshot catalog = mock(DdcServiceCatalogSnapshot.class);
        when(service.register(registration)).thenReturn(session);
        when(service.heartbeat(lease)).thenReturn(renewed);
        when(service.deregister(lease)).thenReturn(deleted);
        when(service.getInstances(key)).thenReturn(snapshot);
        when(service.getServiceKeys(query)).thenReturn(catalog);

        assertThat(facade.register(registration)).isSameAs(session);
        assertThat(facade.heartbeat(lease)).isSameAs(renewed);
        assertThat(facade.deregister(lease)).isSameAs(deleted);
        assertThat(facade.getInstances(key)).isSameAs(snapshot);
        assertThat(facade.getServiceKeys(query)).isSameAs(catalog);

        verify(service).register(registration);
        verify(service).heartbeat(lease);
        verify(service).deregister(lease);
        verify(service).getInstances(key);
        verify(service).getServiceKeys(query);
    }
}
