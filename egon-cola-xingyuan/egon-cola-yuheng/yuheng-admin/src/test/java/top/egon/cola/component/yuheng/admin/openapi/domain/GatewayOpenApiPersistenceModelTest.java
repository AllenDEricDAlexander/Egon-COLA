package top.egon.cola.component.yuheng.admin.openapi.domain;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSnapshotPO;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSyncPO;
import top.egon.cola.component.yuheng.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiSyncKeyDTO;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayOpenApiPersistenceModelTest {

    private static final Instant NOW = Instant.parse(
            "2026-08-26T03:00:00Z"
    );

    @Test
    void snapshotIsAnImmutableValidatedRecord() {
        Map<String, Object> document = new HashMap<>();
        document.put("openapi", "3.1.0");
        List<String> messages = List.of("ok");
        GatewayOpenApiSnapshotPO snapshot = new GatewayOpenApiSnapshotPO(
                "snapshot-1",
                "app-1",
                null,
                "build-1",
                "5.3.3",
                "orders",
                "3.1.0",
                sha('a'),
                sha('b'),
                document,
                "VALID",
                messages,
                2,
                1,
                "instance-1",
                NOW,
                NOW,
                NOW
        );

        document.put("changed", true);

        assertThat(snapshot.documentJson()).doesNotContainKey("changed");
        assertThat(snapshot.validationMessages()).containsExactly("ok");
        assertThatThrownBy(() -> new GatewayOpenApiSnapshotPO(
                "snapshot-1",
                "app-1",
                null,
                "build-1",
                "5.3.3",
                "orders",
                "3.1.0",
                "A".repeat(64),
                sha('b'),
                Map.of(),
                "VALID",
                List.of(),
                0,
                0,
                "instance-1",
                NOW,
                NOW,
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase");
    }

    @Test
    void syncKeyAndStateExposeTheApprovedCasVocabulary() {
        GatewayOpenApiSyncKeyDTO key = new GatewayOpenApiSyncKeyDTO(
                " app-1 ",
                " build-1 ",
                " orders "
        );
        assertThat(key.applicationId()).isEqualTo("app-1");
        assertThat(key.buildId()).isEqualTo("build-1");
        assertThat(key.openapiGroup()).isEqualTo("orders");
        assertThat(GatewayOpenApiSyncStateEnum.values())
                .containsExactly(
                        GatewayOpenApiSyncStateEnum.DISCOVERED,
                        GatewayOpenApiSyncStateEnum.FETCHING,
                        GatewayOpenApiSyncStateEnum.VALIDATING,
                        GatewayOpenApiSyncStateEnum.INVALID,
                        GatewayOpenApiSyncStateEnum.INCONSISTENT_BUILD,
                        GatewayOpenApiSyncStateEnum.INGESTING,
                        GatewayOpenApiSyncStateEnum.VALID,
                        GatewayOpenApiSyncStateEnum.INGEST_FAILED,
                        GatewayOpenApiSyncStateEnum.FETCH_FAILED,
                        GatewayOpenApiSyncStateEnum.STALE
                );
        assertThat(GatewayOpenApiSyncStateEnum.DISCOVERED
                .canTransitionTo(GatewayOpenApiSyncStateEnum.FETCHING))
                .isTrue();
        assertThat(GatewayOpenApiSyncStateEnum.FETCHING
                .canTransitionTo(GatewayOpenApiSyncStateEnum.VALID))
                .isFalse();
        assertThatThrownBy(() -> new GatewayOpenApiSyncKeyDTO(
                "app-1",
                "build-1",
                "1-orders"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase letter");
    }

    @Test
    void syncRecordRejectsInvalidRevisionAndValidReferences() {
        assertThatThrownBy(() -> new GatewayOpenApiSyncPO(
                "sync-1",
                "app-1",
                "build-1",
                "5.3.3",
                "orders",
                "orders-service",
                "default",
                "1.0.0",
                GatewayOpenApiSyncStateEnum.VALID,
                null,
                null,
                null,
                0,
                null,
                null,
                NOW,
                NOW,
                NOW,
                null,
                -1,
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revision");

        GatewayOpenApiSyncPO valid = new GatewayOpenApiSyncPO(
                "sync-1",
                "app-1",
                "build-1",
                "5.3.3",
                "orders",
                "orders-service",
                "default",
                "1.0.0",
                GatewayOpenApiSyncStateEnum.VALID,
                "snapshot-1",
                "set-1",
                "instance-1",
                1,
                null,
                null,
                NOW,
                NOW,
                NOW,
                null,
                2,
                NOW
        );
        assertThat(valid.definitionSetId()).isEqualTo("set-1");
    }

    private static String sha(char value) {
        return String.valueOf(value).repeat(64);
    }
}
