package top.egon.cola.platform.rbac3.admin.iam.application.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.command.AdmitApplicationAuthorizationScopeCommand;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.vo.ApplicationAuthorizationScopeVO;
import top.egon.cola.platform.rbac3.admin.iam.application.repository.ApplicationResourceRepository;
import top.egon.cola.platform.rbac3.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.rbac3.admin.iam.business.service.DdcCatalogGateway;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class ApplicationScopeFacadeTest {

    private final DdcCatalogGateway catalog = mock(DdcCatalogGateway.class);
    private final ApplicationResourceRepository store = mock(ApplicationResourceRepository.class);
    private final ApplicationScopeFacade facade = new ApplicationScopeFacade(catalog, store);

    @Test
    void admitLoadsDirectoryFactsFromDdcRatherThanTheRequest() {
        ApplicationCatalogEntry entry = new ApplicationCatalogEntry(
                "ddc-app-1", "ddc-biz-1", "orders",
                "console", "Console", true, true);
        ApplicationAuthorizationScopeVO admitted = new ApplicationAuthorizationScopeVO(
                "71001", "ddc-biz-1", "ddc-app-1", "orders",
                "console", "Console", "ACTIVE", 100, 0L);
        when(catalog.findApplication("ddc-app-1")).thenReturn(Optional.of(entry));
        when(store.admit(10001L, entry, 100, "actor-1")).thenReturn(admitted);

        ApplicationAuthorizationScopeVO scope = facade.admit(10001L, "actor-1",
                new AdmitApplicationAuthorizationScopeCommand("ddc-app-1", 100));

        assertThat(scope.ddcBusinessId()).isEqualTo("ddc-biz-1");
        assertThat(scope.applicationCode()).isEqualTo("console");
        verify(store).admit(10001L, entry, 100, "actor-1");
    }

    @Test
    void rejectsAdmissionWhenTheParentBusinessIsDisabled() {
        when(catalog.findApplication("ddc-app-1")).thenReturn(Optional.of(
                new ApplicationCatalogEntry(
                        "ddc-app-1", "ddc-biz-1", "orders",
                        "console", "Console", true, false)));

        assertThatThrownBy(() -> facade.admit(10001L, "actor-1",
                new AdmitApplicationAuthorizationScopeCommand("ddc-app-1", 100)))
                .isInstanceOf(IllegalStateException.class);
        verify(store, never()).admit(any(), any(), any(Integer.class), any());
    }
}
