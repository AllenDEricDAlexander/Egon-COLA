package top.egon.cola.component.rag.chunk;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rag.exception.RagValidationException;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagChunkBO;
import top.egon.cola.component.rag.model.RagChunkingConfigDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Locks each built-in strategy's output shape, determinism and parameter guards. */
class RagChunkingStrategyTest {

    private static final String LONG_TEXT = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu "
            + "nu xi omicron pi rho sigma tau upsilon phi chi psi omega ".repeat(40);

    private static final String MARKDOWN = """
            # Title
            Intro line.
            ## Section A
            Content of A.
            ## Section B
            Content of B.
            """;

    @Test
    void token_strategy_splits_long_text_into_contiguous_chunks() {
        List<RagChunkBO> chunks = new TokenRagChunkingStrategy()
                .split(document(LONG_TEXT), config(RagChunkingStrategyEnum.TOKEN, 40, 0, null));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).extracting(RagChunkBO::chunkIndex).containsExactlyElementsOf(indices(chunks.size()));
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.content()).isNotBlank());
    }

    @Test
    void markdown_strategy_splits_on_the_configured_heading_levels() {
        List<RagChunkBO> chunks = new MarkdownHeadingRagChunkingStrategy()
                .split(document(MARKDOWN), config(RagChunkingStrategyEnum.MARKDOWN_HEADING, 512, 0, List.of(1, 2)));

        assertThat(chunks).hasSize(3);
        assertThat(chunks).extracting(RagChunkBO::chunkIndex).containsExactly(0, 1, 2);
        assertThat(chunks).extracting(chunk -> chunk.attributes().get("headingPath"))
                .containsExactly("Title", "Title > Section A", "Title > Section B");
    }

    @Test
    void markdown_strategy_degrades_to_a_single_section_without_headings() {
        List<RagChunkBO> chunks = new MarkdownHeadingRagChunkingStrategy()
                .split(document("plain body only"), config(RagChunkingStrategyEnum.MARKDOWN_HEADING, 512, 0, null));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).attributes()).doesNotContainKey("headingPath");
    }

    @Test
    void recursive_strategy_splits_and_keeps_indices_contiguous() {
        List<RagChunkBO> chunks = new RecursiveRagChunkingStrategy()
                .split(document(LONG_TEXT), config(RagChunkingStrategyEnum.RECURSIVE, 40, 0, null));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).extracting(RagChunkBO::chunkIndex).containsExactlyElementsOf(indices(chunks.size()));
    }

    @Test
    void every_strategy_is_deterministic_for_the_same_input() {
        RagChunkingConfigDTO config = config(RagChunkingStrategyEnum.TOKEN, 40, 5, null);
        RagChunkingStrategy strategy = new TokenRagChunkingStrategy();

        assertThat(strategy.split(document(LONG_TEXT), config)).isEqualTo(strategy.split(document(LONG_TEXT), config));
    }

    @Test
    void rejects_overlap_not_smaller_than_the_chunk_size() {
        assertThatThrownBy(() -> config(RagChunkingStrategyEnum.TOKEN, 100, 100, null))
                .isInstanceOf(RagValidationException.class)
                .hasMessageContaining("overlapTokens");
    }

    @Test
    void rejects_heading_levels_for_a_non_markdown_strategy() {
        assertThatThrownBy(() -> config(RagChunkingStrategyEnum.TOKEN, 100, 0, List.of(1, 2)))
                .isInstanceOf(RagValidationException.class)
                .hasMessageContaining("headingLevels");
    }

    @Test
    void rejects_heading_levels_out_of_range() {
        assertThatThrownBy(() -> config(RagChunkingStrategyEnum.MARKDOWN_HEADING, 100, 0, List.of(0, 9)))
                .isInstanceOf(RagValidationException.class);
    }

    @Test
    void returns_no_chunks_for_blank_content() {
        assertThat(new TokenRagChunkingStrategy()
                .split(document("   "), config(RagChunkingStrategyEnum.TOKEN, 100, 0, null))).isEmpty();
    }

    private static ExtractedDocumentBO document(String text) {
        return new ExtractedDocumentBO(text, "title", "text/plain", null);
    }

    private static RagChunkingConfigDTO config(RagChunkingStrategyEnum strategy, int maxTokens, int overlap,
                                               List<Integer> headingLevels) {
        return new RagChunkingConfigDTO(strategy, maxTokens, overlap, 1, headingLevels);
    }

    private static List<Integer> indices(int size) {
        return java.util.stream.IntStream.range(0, size).boxed().toList();
    }
}
