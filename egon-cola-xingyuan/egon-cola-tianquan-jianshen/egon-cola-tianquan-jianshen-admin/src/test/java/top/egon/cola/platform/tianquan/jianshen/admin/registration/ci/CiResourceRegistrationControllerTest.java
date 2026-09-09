package top.egon.cola.platform.tianquan.jianshen.admin.registration.ci;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.RequiresServiceScope;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.DdcCatalogGateway;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.controller.CiResourceRegistrationController;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.domain.FrontendResourceType;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.domain.dto.CiResourceRegistrationRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.domain.vo.CiResourceRegistrationResultVO;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.service.CiResourceRegistrationCanonicalizer;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.service.CiResourceRegistrationService;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.service.CiResourceRegistrationStore;

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

class CiResourceRegistrationControllerTest {

    private static final Instant NOW = Instant.parse("2026-08-18T02:00:00Z");

    @Test
    void rejectsAUserOrAServiceBoundToAnotherSourceBeforeStoreWrite() {
        DdcCatalogGateway catalog = mock(DdcCatalogGateway.class);
        CiResourceRegistrationStore store = mock(CiResourceRegistrationStore.class);
        CiResourceRegistrationService service = new CiResourceRegistrationService(catalog, store);
        CiResourceRegistrationController controller = new CiResourceRegistrationController(service);

        assertThatThrownBy(() -> controller.register(
                "biz-a", "app-a", null, request("build-1")))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> controller.register(
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
        CiResourceRegistrationStore store = mock(CiResourceRegistrationStore.class);
        CiResourceRegistrationService service = new CiResourceRegistrationService(catalog, store);
        CiResourceRegistrationController controller = new CiResourceRegistrationController(service);
        when(catalog.listApplications("biz-a", null)).thenReturn(List.of(
                new ApplicationCatalogEntry(
                        "tianshu-app-a", "tianshu-biz-a", "biz-a", "app-a",
                        "Application A", true, true)));
        CiResourceRegistrationRequestDTO request = request("build-1");
        CiResourceRegistrationResultVO result = new CiResourceRegistrationResultVO(
                1, 0, 0, 0, 1, 0, 0, request.checksum(), 1L);
        when(store.findHead("app-a")).thenReturn(Optional.empty());
        when(store.replace("app-a", request, request.checksum())).thenReturn(result);

        assertThat(controller.register(
                "biz-a", "app-a", principal("biz-a", "app-a"), request))
                .extracting(response -> response.data()).isEqualTo(result);
        verify(store).replace("app-a", request, request.checksum());

        when(store.findHead("app-a")).thenReturn(Optional.of(
                new CiResourceRegistrationStore.RegistrationHead(
                        request.buildId(), request.checksum(), result)));
        assertThat(controller.register(
                "biz-a", "app-a", principal("biz-a", "app-a"), request))
                .extracting(response -> response.data()).isEqualTo(result);
    }

    @Test
    void registrationEndpointRequiresServiceScopeAndHasNoTenantRequestField() {
        assertThat(CiResourceRegistrationController.class
                .getAnnotation(RequestMapping.class).value()[0])
                .isEqualTo("/api/tianquan-jianshen/v1/registration");
        RequiresServiceScope required = java.util.Arrays.stream(
                        CiResourceRegistrationController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("register"))
                .findFirst()
                .orElseThrow()
                .getAnnotation(RequiresServiceScope.class);
        assertThat(required).isNotNull();
        assertThat(required.value()).isEqualTo("tianquan-jianshen:resource-catalog:report");
        assertThat(CiResourceRegistrationRequestDTO.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals("tenantId"));
        assertThat(CiResourceRegistrationRequestDTO.Resource.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals("permissionCode"));
    }

    private CiResourceRegistrationRequestDTO request(String buildId) {
        CiResourceRegistrationRequestDTO unchecked = new CiResourceRegistrationRequestDTO(
                buildId,
                "sha256:" + "0".repeat(64),
                0L,
                List.of(new CiResourceRegistrationRequestDTO.Resource(
                        FrontendResourceType.MENU, "menu.home", "Home", null,
                        "home:read", List.of(), null, null, null, 1, false)),
                List.of());
        return new CiResourceRegistrationRequestDTO(
                unchecked.buildId(),
                CiResourceRegistrationCanonicalizer.checksum(unchecked),
                unchecked.expectedApplicationVersion(),
                unchecked.resources(),
                unchecked.fields());
    }

    private ServiceIdentityPrincipal principal(String business, String application) {
        return new ServiceIdentityPrincipal(
                "ci-client", "tenant-a", "ci-client", "token-1",
                URI.create("https://tianquan-shoubing.example.test/resource"), 1L,
                Set.of("tianquan-jianshen:resource-catalog:report"), business, application,
                "ci", "kid-1", NOW, NOW.plusSeconds(300));
    }
}
