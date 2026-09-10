package top.egon.cola.component.rag.chunk;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rag.exception.RagConfigurationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Locks the enum-to-strategy selection and the fail-closed behaviour when one is missing. */
class RagChunkingStrategyFactoryTest {

    @Test
    void resolves_all_builtin_strategies() {
        RagChunkingStrategyFactory factory = new RagChunkingStrategyFactory(List.of(
                new TokenRagChunkingStrategy(),
                new MarkdownHeadingRagChunkingStrategy(),
                new RecursiveRagChunkingStrategy()));

        for (RagChunkingStrategyEnum value : RagChunkingStrategyEnum.values()) {
            assertThat(factory.resolve(value).strategy()).isEqualTo(value);
        }
    }

    @Test
    void fails_when_a_strategy_is_not_registered() {
        RagChunkingStrategyFactory factory = new RagChunkingStrategyFactory(List.of(new TokenRagChunkingStrategy()));

        assertThatThrownBy(() -> factory.resolve(RagChunkingStrategyEnum.RECURSIVE))
                .isInstanceOf(RagConfigurationException.class)
                .hasMessageContaining("RECURSIVE")
                .hasMessageContaining("TOKEN");
    }

    @Test
    void rejects_a_null_strategy_value() {
        RagChunkingStrategyFactory factory = new RagChunkingStrategyFactory(List.of(new TokenRagChunkingStrategy()));

        assertThatThrownBy(() -> factory.resolve(null)).isInstanceOf(RagConfigurationException.class);
    }
}
