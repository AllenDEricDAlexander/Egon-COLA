package top.egon.cola.platform.tianquan.jianshen.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.shoubing.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.tianquan.jianshen.admin.audit.controller.AuditController;
import top.egon.cola.platform.tianquan.jianshen.admin.audit.service.AuditQueryService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.decision.controller.InternalAuthorizationController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.decision.service.AuthorizationDecisionService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.participation.controller.ParticipationController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.participation.service.ParticipationFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.controller.RuntimeController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.RuntimeQueryService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.simulation.controller.AuthorizationSimulationController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.simulation.service.AuthorizationSimulationService;

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
                        "tianquan-jianshen-internal-system-snapshot-v2",
                        "tianquan-jianshen-internal-authorization-decision-v2",
                        "tianquan-jianshen-internal-resource-access-decision-v2",
                        "tianquan-jianshen-internal-authorization-fence-verify-v2");
        assertThat(operationIds(ParticipationController.class))
                .contains(
                        "tianquan-jianshen-business-participation-record-v1",
                        "tianquan-jianshen-business-participation-conflicts-v1");
        assertThat(operationIds(AuditController.class))
                .contains("tianquan-jianshen-audit-log-list-v1");
        assertThat(operationIds(AuthorizationSimulationController.class))
                .contains(
                        "tianquan-jianshen-authorization-simulation-v1",
                        "tianquan-jianshen-role-change-impact-simulation-v1");
        assertThat(operationIds(RuntimeController.class))
                .contains(
                        "tianquan-jianshen-runtime-status-v1",
                        "tianquan-jianshen-runtime-mutations-v1",
                        "tianquan-jianshen-runtime-mutation-retry-v1",
                        "tianquan-jianshen-runtime-yuheng-tianshu-status-v1");
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
