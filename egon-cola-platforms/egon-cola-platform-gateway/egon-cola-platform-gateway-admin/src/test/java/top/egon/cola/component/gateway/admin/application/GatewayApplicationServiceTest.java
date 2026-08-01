package top.egon.cola.component.gateway.admin.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ddc.management.model.DdcManagementScopeBinding;
import top.egon.cola.component.gateway.admin.application.scope.GatewayScopeService;
import top.egon.cola.component.gateway.admin.application.scope.GatewayScopeService.ScopeQuery;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;

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
        ScopeQuery scope = new ScopeQuery(
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
        ScopeQuery scope = new ScopeQuery(
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
        ScopeQuery query = new ScopeQuery(null, null, null, null);
        when(scopes.bindings(query)).thenReturn(List.of(binding("ops")));
        when(applications.findAllByDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(
                        application("application-order", "default"),
                        new GatewayApplicationEntity(
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
                        GatewayApplicationService.GatewayApplicationView::id,
                        GatewayApplicationService.GatewayApplicationView::ddcMatched
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

    private GatewayApplicationService.CreateGatewayApplication command(
            ScopeQuery scope) {
        return new GatewayApplicationService.CreateGatewayApplication(
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

    private GatewayApplicationEntity application(
            String id,
            String namespace) {
        return new GatewayApplicationEntity(
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
                AdminActor.ActorType.USER,
                Set.of("*"),
                Set.of("GATEWAY_ADMIN")
        );
    }

    private RequestAuditContext request() {
        return new RequestAuditContext("request", "trace");
    }
}
