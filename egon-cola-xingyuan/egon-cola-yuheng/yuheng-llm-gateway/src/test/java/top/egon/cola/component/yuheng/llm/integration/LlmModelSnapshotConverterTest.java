package top.egon.cola.component.yuheng.llm.integration;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmCapabilityEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.po.LlmChannelPO;
import top.egon.cola.component.yuheng.llm.proxy.domain.po.LlmModelPO;
import top.egon.cola.component.yuheng.llm.proxy.repository.LlmModelSnapshotConverter;

import static org.assertj.core.api.Assertions.assertThat;

class LlmModelSnapshotConverterTest {

    private static final Instant MODEL_CREATED_AT = Instant.parse("2026-09-21T08:00:00.123456Z");

    private final LlmModelSnapshotConverter converter = new LlmModelSnapshotConverter();

    @Test
    @DisplayName("MapStruct projection preserves nullable chat dimensions, embedding space and unauthenticated local secretRef")
    void projectionPreservesNullablePersistenceFieldsAndResolvesTheDeclaredRoute() {
        LlmModelPO model = LlmModelPO.builder()
                .modelKey("company-chat")
                .name("Company Chat")
                .createTime(MODEL_CREATED_AT)
                .kind(LlmModelKindEnum.CHAT)
                .enabled(Boolean.TRUE)
                .dimensions(null)
                .embeddingSpaceId(null)
                .revision(7L)
                .build();
        LlmChannelPO channel = LlmChannelPO.builder()
                .channelKey("local-main")
                .name("Local Main")
                .deployment(LlmDeploymentEnum.LOCAL)
                .protocol(LlmProtocolEnum.OPENAI_CHAT)
                .baseUrl("http://127.0.0.1:8000/v1")
                .secretRef(null)
                .enabled(Boolean.TRUE)
                .connectTimeoutMs(2_000)
                .headerTimeoutMs(30_000)
                .idleTimeoutMs(60_000)
                .totalTimeoutMs(120_000)
                .maxConcurrent(4)
                .build();
        LlmModelSnapshotBO.RouteBO route = converter.toRoute(
                "local-main", "qwen-local", 1, 100, Set.of(LlmCapabilityEnum.TEXT));

        LlmModelSnapshotBO snapshot = converter.toTarget(converter.toProjection(
                model,
                List.of(LlmProtocolEnum.OPENAI_CHAT),
                List.of("svc:wiki-indexer"),
                List.of(converter.withChannel(route, converter.toChannel(channel)))));

        assertThat(snapshot.getModelKey()).isEqualTo("company-chat");
        assertThat(snapshot.getCreatedAt()).isEqualTo(MODEL_CREATED_AT);
        assertThat(snapshot.getDimensions()).isNull();
        assertThat(snapshot.getEmbeddingSpaceId()).isNull();
        assertThat(snapshot.getProtocols()).containsExactly(LlmProtocolEnum.OPENAI_CHAT);
        assertThat(snapshot.getAllowedSubjects()).containsExactly("svc:wiki-indexer");
        assertThat(snapshot.getRoutes()).hasSize(1);
        assertThat(snapshot.getRoutes().getFirst().getChannel().getSecretRef()).isNull();
        assertThat(snapshot.getRoutes().getFirst().getChannel().getDeployment()).isEqualTo(LlmDeploymentEnum.LOCAL);
    }

    @Test
    @DisplayName("A route to a missing channel remains unresolved instead of widening to another channel")
    void projectionPreservesAnUnresolvedRoute() {
        LlmModelSnapshotBO.RouteBO route = converter.toRoute(
                "missing-channel", "qwen-local", 1, 100, Set.of(LlmCapabilityEnum.TEXT));

        assertThat(converter.withChannel(route, null).getChannel()).isNull();
    }
}
