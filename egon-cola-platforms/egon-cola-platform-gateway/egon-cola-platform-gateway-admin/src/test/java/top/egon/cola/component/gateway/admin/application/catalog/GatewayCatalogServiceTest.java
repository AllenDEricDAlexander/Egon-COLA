package top.egon.cola.component.gateway.admin.application.catalog;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;

import java.time.Clock;
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

class GatewayCatalogServiceTest {

    private static final Instant NOW = Instant.parse(
            "2026-07-25T00:00:00Z"
    );

    @Test
    void createsManualHttpOperationWithStableIdentityAndVersion() {
        FakeStore store = new FakeStore();
        GatewayCatalogService service = service(store);

        GatewayCatalogService.OperationDetail created =
                service.createManualOperation(
                        "group-1",
                        operation(false),
                        actor(),
                        audit()
                );

        assertThat(created.operation().operationKey())
                .isEqualTo("orders:http:GET:/orders/{id}");
        assertThat(created.operation().externalAccessible()).isFalse();
        assertThat(created.operation().sourceType()).isEqualTo("MANUAL");
        assertThat(created.definitions()).singleElement()
                .satisfies(definition -> {
                    assertThat(definition.definitionVersion()).isOne();
                    assertThat(definition.externalAccessible()).isFalse();
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
        ).operation().id();

        GatewayCatalogService.OperationDetail updated =
                service.updateManualDefinition(
                        operationId,
                        definition(true, "updated"),
                        actor(),
                        audit()
                );

        assertThat(updated.operation().operationKey())
                .isEqualTo("orders:http:GET:/orders/{id}");
        assertThat(updated.operation().externalAccessible()).isTrue();
        assertThat(updated.definitions())
                .extracting(GatewayCatalogStore.OperationDefinition
                        ::definitionVersion)
                .containsExactly(2L, 1L);
    }

    @Test
    void refusesManualOverwriteOfStarterOperation() {
        FakeStore store = new FakeStore();
        store.operation = new GatewayCatalogStore.OperationRecord(
                "starter-operation",
                "application-1",
                "group-1",
                "orders:http:GET:/orders/{id}",
                "HTTP",
                "GET /orders/{id}",
                false,
                Map.of(),
                "STARTER",
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
                .hasMessageContaining("STARTER");
    }

    private GatewayCatalogService service(FakeStore store) {
        return new GatewayCatalogService(
                store,
                mock(GatewayAuditLogRepository.class),
                JsonMapper.builder().build(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private GatewayCatalogService.ManualOperation operation(
            boolean externalAccessible) {
        return new GatewayCatalogService.ManualOperation(
                GatewayCatalogService.Protocol.HTTP,
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

    private GatewayCatalogService.ManualDefinition definition(
            boolean externalAccessible,
            String summary) {
        return new GatewayCatalogService.ManualDefinition(
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
                AdminActor.ActorType.USER,
                Set.of("*"),
                Set.of("GATEWAY_ADMIN")
        );
    }

    private RequestAuditContext audit() {
        return new RequestAuditContext("request", "trace");
    }

    private static final class FakeStore implements GatewayCatalogStore {

        private final List<OperationDefinition> definitions =
                new ArrayList<>();

        private OperationRecord operation;

        @Override
        public CatalogTree loadCatalog(String applicationId) {
            return new CatalogTree(applicationId, List.of());
        }

        @Override
        public String createManualHierarchy(
                String applicationId,
                ManualHierarchy hierarchy,
                Instant now) {
            return "group-1";
        }

        @Override
        public Optional<InterfaceGroupScope> findInterfaceGroup(String id) {
            return Optional.of(new InterfaceGroupScope(
                    id,
                    "application-1",
                    "test-biz",
                    "orders",
                    "test",
                    "default"
            ));
        }

        @Override
        public Optional<OperationRecord> findOperation(String operationId) {
            return operation == null || !operation.id().equals(operationId)
                    ? Optional.empty()
                    : Optional.of(operation);
        }

        @Override
        public Optional<OperationRecord> findOperation(
                String applicationId,
                String operationKey) {
            return operation == null
                    || !operation.operationKey().equals(operationKey)
                    ? Optional.empty()
                    : Optional.of(operation);
        }

        @Override
        public List<OperationDefinition> loadDefinitions(String operationId) {
            return definitions.reversed();
        }

        @Override
        public void insertOperation(OperationRecord value) {
            operation = value;
        }

        @Override
        public void appendDefinition(OperationDefinition definition) {
            definitions.add(definition);
        }

        @Override
        public void pointToDefinition(
                String operationId,
                String definitionId,
                boolean externalAccessible,
                Instant now) {
            operation = new OperationRecord(
                    operation.id(),
                    operation.applicationId(),
                    operation.interfaceGroupId(),
                    operation.operationKey(),
                    operation.protocol(),
                    operation.methodIdentity(),
                    externalAccessible,
                    new LinkedHashMap<>(
                            operation.providerServiceIdentity()
                    ),
                    operation.sourceType(),
                    "ACTIVE",
                    definitionId,
                    operation.revision() + 1,
                    operation.createdAt(),
                    now
            );
        }

        @Override
        public void deprecate(String operationId, Instant now) {
        }
    }
}
