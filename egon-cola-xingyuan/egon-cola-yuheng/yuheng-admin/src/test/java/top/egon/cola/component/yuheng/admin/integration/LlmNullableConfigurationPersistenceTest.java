package top.egon.cola.component.yuheng.admin.integration;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import top.egon.cola.component.yuheng.admin.llm.converter.LlmChannelPersistenceConverter;
import top.egon.cola.component.yuheng.admin.llm.converter.LlmModelPersistenceConverter;
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmChannelBO;
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmModelBO;
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmRouteBindingDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmCapabilityEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.po.LlmChannelPO;
import top.egon.cola.component.yuheng.admin.llm.domain.po.LlmModelPO;

import static org.assertj.core.api.Assertions.assertThat;

class LlmNullableConfigurationPersistenceTest {

    @Test
    @DisplayName("完整替换把 nullable secret_ref/dimensions/embedding_space_id 映射成 SQL NULL 并要求 MP 始终更新")
    void fullReplacementPreservesNullsForMyBatisPlusUpdateById() throws NoSuchFieldException {
        LlmChannelPO channelRow = LlmChannelPO.builder()
                .id(11L)
                .channelKey("local-main")
                .secretRef("llm/local-main")
                .build();
        LlmChannelBO channel = LlmChannelBO.builder()
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
                .revision(1L)
                .build();
        new LlmChannelPersistenceConverter().applyBusiness(channel, channelRow);

        assertThat(channelRow.getSecretRef()).isNull();
        assertThat(updateStrategy(LlmChannelPO.class, "secretRef")).isEqualTo(FieldStrategy.ALWAYS);

        LlmModelPO modelRow = LlmModelPO.builder()
                .id(12L)
                .modelKey("company-chat")
                .dimensions(1024)
                .embeddingSpaceId("local:old-space")
                .build();
        LlmModelBO model = LlmModelBO.builder()
                .modelKey("company-chat")
                .name("Company Chat")
                .kind(LlmModelKindEnum.CHAT)
                .protocols(List.of(LlmProtocolEnum.OPENAI_CHAT))
                .enabled(Boolean.TRUE)
                .dimensions(null)
                .embeddingSpaceId(null)
                .allowedSubjects(List.of("svc:wiki-indexer"))
                .routes(List.of(new LlmRouteBindingDTO(
                        "local-main", "qwen-local", java.util.Set.of(LlmCapabilityEnum.TEXT), 1, 100)))
                .revision(1L)
                .build();
        new LlmModelPersistenceConverter().applyBusiness(model, modelRow);

        assertThat(modelRow.getDimensions()).isNull();
        assertThat(modelRow.getEmbeddingSpaceId()).isNull();
        assertThat(updateStrategy(LlmModelPO.class, "dimensions")).isEqualTo(FieldStrategy.ALWAYS);
        assertThat(updateStrategy(LlmModelPO.class, "embeddingSpaceId")).isEqualTo(FieldStrategy.ALWAYS);
    }

    private static FieldStrategy updateStrategy(Class<?> type, String fieldName) throws NoSuchFieldException {
        Field field = type.getDeclaredField(fieldName);
        TableField tableField = field.getAnnotation(TableField.class);
        assertThat(tableField).as("MyBatis field mapping for %s.%s", type.getSimpleName(), fieldName).isNotNull();
        return tableField.updateStrategy();
    }
}
