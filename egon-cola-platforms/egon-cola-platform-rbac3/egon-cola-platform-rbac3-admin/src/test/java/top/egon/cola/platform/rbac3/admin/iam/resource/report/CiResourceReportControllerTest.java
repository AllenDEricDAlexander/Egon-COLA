package top.egon.cola.platform.rbac3.admin.iam.resource.report;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;
import top.egon.cola.platform.rbac3.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.rbac3.admin.iam.business.service.DdcCatalogGateway;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.controller.CiResourceReportController;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.FrontendResourceType;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.dto.CiResourceReportRequestDTO;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.domain.vo.CiResourceReportResultVO;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.service.CiResourceReportCanonicalizer;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.service.CiResourceReportService;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.service.CiResourceReportStore;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CiResourceReportControllerTest {

    private static final Instant NOW = Instant.parse("2026-08-18T02:00:00Z");

    @Test
    void rejectsAUserOrAServiceBoundToAnotherSourceBeforeStoreWrite() {
        DdcCatalogGateway catalog = mock(DdcCatalogGateway.class);
        CiResourceReportStore store = mock(CiResourceReportStore.class);
        CiResourceReportService service = new CiResourceReportService(catalog, store);
        CiResourceReportController controller = new CiResourceReportController(service);

        assertThatThrownBy(() -> controller.report(
                "biz-a", "app-a", null, request("build-1")))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> controller.report(
                "biz-a", "app-a", principal("biz-b", "app-a"), request("build-1")))
                .isInstanceOf(SecurityException.class);
        verify(store, never()).replace(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void acceptsBoundServiceAndMakesSameBuildReplayIdempotent() {
        DdcCatalogGateway catalog = mock(DdcCatalogGateway.class);
        CiResourceReportStore store = mock(CiResourceReportStore.class);
        CiResourceReportService service = new CiResourceReportService(catalog, store);
        CiResourceReportController controller = new CiResourceReportController(service);
        when(catalog.listApplications("biz-a", null)).thenReturn(List.of(
                new ApplicationCatalogEntry(
                        "ddc-app-a", "ddc-biz-a", "biz-a", "app-a",
                        "Application A", true, true)));
        CiResourceReportRequestDTO request = request("build-1");
        CiResourceReportResultVO result = new CiResourceReportResultVO(
                1, 0, 0, 0, 1, request.checksum());
        when(store.findHead("app-a")).thenReturn(Optional.empty());
        when(store.replace("app-a", request, request.checksum())).thenReturn(result);

        assertThat(controller.report(
                "biz-a", "app-a", principal("biz-a", "app-a"), request))
                .isEqualTo(result);
        verify(store).replace("app-a", request, request.checksum());

        when(store.findHead("app-a")).thenReturn(Optional.of(
                new CiResourceReportStore.ReportHead(
                        request.buildId(), request.checksum(), result)));
        assertThat(controller.report(
                "biz-a", "app-a", principal("biz-a", "app-a"), request))
                .isEqualTo(result);
    }

    @Test
    void reportEndpointRequiresServiceScopeAndHasNoTenantRequestField() {
        RequiresServiceScope required = java.util.Arrays.stream(
                        CiResourceReportController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("report"))
                .findFirst()
                .orElseThrow()
                .getAnnotation(RequiresServiceScope.class);
        assertThat(required).isNotNull();
        assertThat(required.value()).isEqualTo("rbac3:resource-catalog:report");
        assertThat(CiResourceReportRequestDTO.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals("tenantId"));
    }

    private CiResourceReportRequestDTO request(String buildId) {
        CiResourceReportRequestDTO unchecked = new CiResourceReportRequestDTO(
                buildId,
                "sha256:" + "0".repeat(64),
                0L,
                List.of(new CiResourceReportRequestDTO.Resource(
                        FrontendResourceType.MENU, "menu.home", "Home", null,
                        "home:read", null, null, null, 1, false)),
                List.of());
        return new CiResourceReportRequestDTO(
                unchecked.buildId(),
                CiResourceReportCanonicalizer.checksum(unchecked),
                unchecked.expectedApplicationVersion(),
                unchecked.resources(),
                unchecked.fields());
    }

    private ServiceIdentityPrincipal principal(String business, String application) {
        return new ServiceIdentityPrincipal(
                "ci-client", "tenant-a", "ci-client", "token-1",
                URI.create("https://idp.example.test/resource"), 1L,
                Set.of("rbac3:resource-catalog:report"), business, application,
                "ci", "kid-1", NOW, NOW.plusSeconds(300));
    }
}
