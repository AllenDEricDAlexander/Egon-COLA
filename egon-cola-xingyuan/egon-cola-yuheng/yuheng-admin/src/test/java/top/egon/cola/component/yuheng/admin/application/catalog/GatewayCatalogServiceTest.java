package top.egon.cola.component.yuheng.admin.catalog.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.yuheng.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.yuheng.admin.catalog.repository.*;
import top.egon.cola.component.yuheng.admin.catalog.domain.dto.*;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.*;
import top.egon.cola.component.yuheng.admin.catalog.domain.vo.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import top.egon.cola.component.yuheng.admin.catalog.domain.bo.GatewayOperationBO;
import top.egon.cola.component.yuheng.admin.catalog.domain.bo.GatewayOperationDefinitionBO;

class GatewayCatalogServiceTest {

    private static final Instant NOW = Instant.parse(
            "2026-07-25T00:00:00Z"
    );

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    @Test
    void createsManualHttpOperationWithStableIdentityAndVersion() {
        FakeStore store = new FakeStore();
        GatewayCatalogService service = service(store);

        top.egon.cola.component.yuheng.admin.catalog.domain.vo.GatewayOperationDetailVO created =
                service.createManualOperation(
                        "group-1",
                        operation(false),
                        actor(),
                        audit()
                );

        assertThat(created.operation().getOperationKey())
                .isEqualTo("orders:http:GET:/orders/{id}");
        assertThat(Long.parseLong(created.operation().getId())).isPositive();
        assertThat(created.operation().isExternalAccessible()).isFalse();
        assertThat(created.operation().getSourceType()).isEqualTo("MANUAL");
        assertThat(created.definitions()).singleElement()
                .satisfies(definition -> {
                    assertThat(definition.getDefinitionVersion()).isOne();
                    assertThat(definition.isExternalAccessible()).isFalse();
                });
    }

    @Test
    void appendsDefinitionAndDoesNotRewriteOperationIdentity() {
        FakeStore store = new FakeStore();
        GatewayCatalogService service = service(store);
        String operationId = service.createManualOperation(
                "group-1",
                operation(false),
                actor(),
                audit()
        ).operation().getId();

        top.egon.cola.component.yuheng.admin.catalog.domain.vo.GatewayOperationDetailVO updated =
                service.updateManualDefinition(
                        operationId,
                        definition(true, "updated"),
                        actor(),
                        audit()
                );

        assertThat(updated.operation().getOperationKey())
                .isEqualTo("orders:http:GET:/orders/{id}");
        assertThat(updated.operation().isExternalAccessible()).isTrue();
        assertThat(updated.definitions())
                .extracting(top.egon.cola.component.yuheng.admin.catalog.domain.bo.GatewayOperationDefinitionBO
                        ::getDefinitionVersion)
                .containsExactly(2L, 1L);
    }

    @Test
    void refusesManualOverwriteOfStarterOperation() {
        FakeStore store = new FakeStore();
        store.operation = new top.egon.cola.component.yuheng.admin.catalog.domain.bo.GatewayOperationBO(
                "starter-operation",
                "application-1",
                "group-1",
                "orders:http:GET:/orders/{id}",
                "HTTP",
                "GET /orders/{id}",
                false,
                Map.of(),
                "RPC_DESCRIPTOR",
                "ACTIVE",
                "definition-1",
                1,
                NOW,
                NOW
        );
        GatewayCatalogService service = service(store);

        assertThatThrownBy(() -> service.createManualOperation(
                "group-1",
                operation(false),
                actor(),
                audit()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RPC_DESCRIPTOR");
    }

    private GatewayCatalogService service(FakeStore store) {
        return new GatewayCatalogService(
                store,
                mock(GatewayAuditLogRepository.class),
                JsonMapper.builder().build(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private top.egon.cola.component.yuheng.admin.catalog.domain.dto.GatewayManualOperationDTO operation(
            boolean externalAccessible) {
        return new top.egon.cola.component.yuheng.admin.catalog.domain.dto.GatewayManualOperationDTO(
                top.egon.cola.component.yuheng.admin.catalog.domain.enums.GatewayCatalogProtocolEnum.HTTP,
                "GET",
                "/orders/{id}",
                null,
                null,
                "order-provider",
                "default",
                "1.0.0",
                "HTTP",
                externalAccessible,
                definition(externalAccessible, "initial")
        );
    }

    private top.egon.cola.component.yuheng.admin.catalog.domain.dto.GatewayManualDefinitionDTO definition(
            boolean externalAccessible,
            String summary) {
        return new top.egon.cola.component.yuheng.admin.catalog.domain.dto.GatewayManualDefinitionDTO(
                summary,
                List.of("order"),
                Map.of("type", "object"),
                Map.of("type", "object"),
                List.of(),
                null,
                Map.of(),
                externalAccessible
        );
    }

    private AdminActor actor() {
        return new AdminActor(
                "admin",
                top.egon.cola.component.yuheng.admin.shared.domain.enums.AdminActorTypeEnum.USER,
                Set.of("*"),
                Set.of("YUHENG_ADMIN")
        );
    }

    private RequestAuditContext audit() {
        return new RequestAuditContext("request", "trace");
    }

    private static final class FakeStore implements GatewayCatalogRepository {

        private final List<GatewayOperationDefinitionBO> definitions =
                new ArrayList<>();

        private GatewayOperationBO operation;

        @Override
        public GatewayCatalogTreeVO loadCatalog(String applicationId) {
            return new GatewayCatalogTreeVO(applicationId, List.of());
        }

        @Override
        public String createManualHierarchy(
                String applicationId,
                GatewayManualHierarchyDTO hierarchy,
                Instant now) {
            return "group-1";
        }

        @Override
        public Optional<GatewayInterfaceGroupScopeVO> findInterfaceGroup(String id) {
            return Optional.of(new GatewayInterfaceGroupScopeVO(
                    id,
                    "application-1",
                    "test-biz",
                    "orders",
                    "test",
                    "default"
            ));
        }

        @Override
        public Optional<GatewayOperationBO> findOperation(String operationId) {
            return operation == null || !operation.getId().equals(operationId)
                    ? Optional.empty()
                    : Optional.of(operation);
        }

        @Override
        public Optional<GatewayOperationBO> findOperation(
                String applicationId,
                String operationKey) {
            return operation == null
                    || !operation.getOperationKey().equals(operationKey)
                    ? Optional.empty()
                    : Optional.of(operation);
        }

        @Override
        public List<GatewayOperationDefinitionBO> loadDefinitions(String operationId) {
            return definitions.reversed();
        }

        @Override
        public List<GatewayCurrentOperationDefinitionVO> loadCurrentOperationDefinitions(
                String gatewayGroupId) {
            return List.of();
        }

        @Override
        public void insertOperation(GatewayOperationBO value) {
            operation = value;
        }

        @Override
        public void appendDefinition(GatewayOperationDefinitionBO definition) {
            definitions.add(definition);
        }

        @Override
        public void pointToDefinition(
                String operationId,
                String definitionId,
                boolean externalAccessible,
                Instant now) {
            operation = new GatewayOperationBO(
                    operation.getId(),
                    operation.getApplicationId(),
                    operation.getInterfaceGroupId(),
                    operation.getOperationKey(),
                    operation.getProtocol(),
                    operation.getMethodIdentity(),
                    externalAccessible,
                    new LinkedHashMap<>(
                            operation.getProviderServiceIdentity()
                    ),
                    operation.getSourceType(),
                    "ACTIVE",
                    definitionId,
                    operation.getRevision() + 1,
                    operation.getCreatedAt(),
                    now
            );
        }

        @Override
        public void deprecate(String operationId, Instant now) {
        }
    }
}
