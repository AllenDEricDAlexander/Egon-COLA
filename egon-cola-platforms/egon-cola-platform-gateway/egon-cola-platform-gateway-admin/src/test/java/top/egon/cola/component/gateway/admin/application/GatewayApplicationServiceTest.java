package top.egon.cola.component.gateway.admin.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeBinding;
import top.egon.cola.component.gateway.admin.scope.service.GatewayScopeService;
import top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.domain.exception.GatewayApplicationAlreadyExistsException;
import top.egon.cola.component.gateway.admin.application.domain.po.GatewayApplicationPO;
import top.egon.cola.component.gateway.admin.application.repository.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayAuditLogRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayApplicationServiceTest {

    private GatewayApplicationRepository applications;

    private GatewayAuditLogRepository audits;

    private GatewayScopeService scopes;

    private GatewayApplicationService service;

    @BeforeEach
    void setUp() {
        applications = mock(GatewayApplicationRepository.class);
        audits = mock(GatewayAuditLogRepository.class);
        scopes = mock(GatewayScopeService.class);
        service = new GatewayApplicationService(
                applications,
                audits,
                scopes,
                Clock.fixed(
                        Instant.parse("2026-08-01T00:00:00Z"),
                        ZoneOffset.UTC
                )
        );
    }

    @Test
    void springSelectsTheProductionConstructor() {
        new ApplicationContextRunner()
                .withBean(
                        GatewayApplicationRepository.class,
                        () -> mock(GatewayApplicationRepository.class)
                )
                .withBean(
                        GatewayAuditLogRepository.class,
                        () -> mock(GatewayAuditLogRepository.class)
                )
                .withBean(
                        GatewayScopeService.class,
                        () -> mock(GatewayScopeService.class)
                )
                .withBean(GatewayApplicationService.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(GatewayApplicationService.class));
    }

    @Test
    void secondNamespaceCannotCreateASecondPhysicalApplication() {
        GatewayScopeQueryDTO scope = new GatewayScopeQueryDTO(
                "retail",
                "ops",
                "local",
                "order"
        );
        when(scopes.requireEnabled(scope)).thenReturn(binding("ops"));
        when(applications
                .findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse(
                        "retail",
                        "order",
                        "local"
                ))
                .thenReturn(Optional.of(application(
                        "application-order",
                        "default"
                )));

        assertThatThrownBy(() -> service.create(
                command(scope),
                actor(),
                request()
        )).isInstanceOfSatisfying(
                GatewayApplicationAlreadyExistsException.class,
                error -> assertThat(error.existingApplicationId())
                        .isEqualTo("application-order")
        );
    }

    @Test
    void listsOnePhysicalApplicationThroughEitherNamespace() {
        GatewayScopeQueryDTO scope = new GatewayScopeQueryDTO(
                "retail",
                "ops",
                "local",
                "order"
        );
        when(scopes.bindings(scope)).thenReturn(List.of(binding("ops")));
        when(applications.findAllByDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(application(
                        "application-order",
                        "default"
                )));

        assertThat(service.list(scope))
                .singleElement()
                .satisfies(view -> {
                    assertThat(view.id()).isEqualTo("application-order");
                    assertThat(view.namespace()).isEqualTo("ops");
                    assertThat(view.ddcMatched()).isTrue();
                });
    }

    @Test
    void unfilteredListKeepsLegacyApplicationsAndMarksDdcMatches() {
        GatewayScopeQueryDTO query = new GatewayScopeQueryDTO(null, null, null, null);
        when(scopes.bindings(query)).thenReturn(List.of(binding("ops")));
        when(applications.findAllByDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(
                        application("application-order", "default"),
                        new GatewayApplicationPO(
                                "application-legacy",
                                "legacy",
                                "legacy-app",
                                "Legacy",
                                "local",
                                "default",
                                null,
                                "admin",
                                Instant.parse("2026-08-01T00:00:00Z")
                        )
                ));

        assertThat(service.list(query))
                .extracting(
                        top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO::id,
                        top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO::ddcMatched
                )
                .containsExactly(
                        tuple(
                                "application-order",
                                true
                        ),
                        tuple(
                                "application-legacy",
                                false
                        )
                );
    }

    private top.egon.cola.component.gateway.admin.application.domain.dto.GatewayApplicationCreateCommandDTO command(
            GatewayScopeQueryDTO scope) {
        return new top.egon.cola.component.gateway.admin.application.domain.dto.GatewayApplicationCreateCommandDTO(
                scope.bizCode(),
                scope.appCode(),
                "Order",
                scope.env(),
                scope.namespace(),
                null
        );
    }

    private DdcManagementScopeBinding binding(String namespace) {
        return new DdcManagementScopeBinding(
                "binding-" + namespace,
                "retail",
                namespace,
                "local",
                "ddc-order",
                "order",
                "Order",
                true
        );
    }

    private GatewayApplicationPO application(
            String id,
            String namespace) {
        return new GatewayApplicationPO(
                id,
                "retail",
                "order",
                "Order",
                "local",
                namespace,
                null,
                "admin",
                Instant.parse("2026-08-01T00:00:00Z")
        );
    }

    private AdminActor actor() {
        return new AdminActor(
                "admin",
                top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum.USER,
                Set.of("*"),
                Set.of("GATEWAY_ADMIN")
        );
    }

    private RequestAuditContext request() {
        return new RequestAuditContext("request", "trace");
    }
}
