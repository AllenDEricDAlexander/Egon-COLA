package top.egon.cola.platform.rbac3.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.idp.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.rbac3.admin.audit.controller.AuditController;
import top.egon.cola.platform.rbac3.admin.audit.service.AuditQueryService;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.decision.controller.InternalAuthorizationController;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.decision.service.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.authorization.policy.participation.controller.ParticipationController;
import top.egon.cola.platform.rbac3.admin.authorization.policy.participation.service.ParticipationFacade;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.controller.RuntimeController;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.service.RuntimeQueryService;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.service.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.rbac3.admin.authorization.simulation.controller.AuthorizationSimulationController;
import top.egon.cola.platform.rbac3.admin.authorization.simulation.service.AuthorizationSimulationService;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(
        controllers = {
                InternalAuthorizationController.class,
                ParticipationController.class,
                AuditController.class,
                AuthorizationSimulationController.class,
                RuntimeController.class
        },
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
class Rbac3DecisionRuntimeGatewayDiscoveryTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMappings;

    @MockitoBean
    private AuthorizationDecisionService decisionService;

    @MockitoBean
    private SystemAuthorizationSnapshotService systemAuthorizationSnapshotService;

    @MockitoBean
    private UserAccessTokenVerifier userAccessTokenVerifier;

    @MockitoBean
    private ParticipationFacade participationFacade;

    @MockitoBean
    private AuditQueryService auditQueryService;

    @MockitoBean
    private AuthorizationSimulationService simulationService;

    @MockitoBean
    private RuntimeQueryService runtimeQueryService;

    @Test
    void exposesInternalDecisionParticipationAuditSimulationAndRuntimeOperations() {
        assertThat(operationIds(InternalAuthorizationController.class))
                .contains(
                        "rbac3-internal-system-snapshot-v2",
                        "rbac3-internal-authorization-decision-v2",
                        "rbac3-internal-resource-access-decision-v2",
                        "rbac3-internal-authorization-fence-verify-v2");
        assertThat(operationIds(ParticipationController.class))
                .contains(
                        "rbac3-business-participation-record-v1",
                        "rbac3-business-participation-conflicts-v1");
        assertThat(operationIds(AuditController.class))
                .contains("rbac3-audit-log-list-v1");
        assertThat(operationIds(AuthorizationSimulationController.class))
                .contains(
                        "rbac3-authorization-simulation-v1",
                        "rbac3-role-change-impact-simulation-v1");
        assertThat(operationIds(RuntimeController.class))
                .contains(
                        "rbac3-runtime-status-v1",
                        "rbac3-runtime-mutations-v1",
                        "rbac3-runtime-mutation-retry-v1",
                        "rbac3-runtime-gateway-ddc-status-v1");
        assertThat(exposures(InternalAuthorizationController.class))
                .containsOnly(EgonGatewayPolicy.Exposure.INTERNAL);
        assertThat(exposures(ParticipationController.class))
                .containsOnly(EgonGatewayPolicy.Exposure.INTERNAL);
    }

    private Set<String> operationIds(Class<?> controllerType) {
        return handlerMappings.getHandlerMethods().values().stream()
                .filter(handler -> controllerType.equals(handler.getBeanType()))
                .map(handler -> handler.getMethod().getAnnotation(Operation.class))
                .filter(Objects::nonNull)
                .map(Operation::operationId)
                .collect(Collectors.toSet());
    }

    private Set<EgonGatewayPolicy.Exposure> exposures(Class<?> controllerType) {
        return handlerMappings.getHandlerMethods().values().stream()
                .filter(handler -> controllerType.equals(handler.getBeanType()))
                .map(handler -> handler.getMethod().getAnnotation(EgonGatewayPolicy.class))
                .filter(Objects::nonNull)
                .map(EgonGatewayPolicy::exposure)
                .collect(Collectors.toSet());
    }
}
