package top.egon.cola.component.rag.chunk;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rag.exception.RagValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Locks the deterministic chunk identity that makes ingestion safe to re-run. */
class RagChunkIdFactoryTest {

    private final RagChunkIdFactory factory = new RagChunkIdFactory();

    @Test
    void generates_the_same_id_for_the_same_document_and_index() {
        assertThat(factory.create("doc-1", 7)).isEqualTo("doc-1:7").isEqualTo(factory.create("doc-1", 7));
    }

    @Test
    void distinct_indices_produce_distinct_ids() {
        assertThat(factory.create("doc-1", 0)).isNotEqualTo(factory.create("doc-1", 1));
    }

    @Test
    void rejects_blank_document_id() {
        assertThatThrownBy(() -> factory.create(" ", 0)).isInstanceOf(RagValidationException.class);
    }

    @Test
    void rejects_document_id_containing_colon() {
        assertThatThrownBy(() -> factory.create("doc:1", 0))
                .isInstanceOf(RagValidationException.class)
                .hasMessageContaining(":");
    }

    @Test
    void rejects_negative_chunk_index() {
        assertThatThrownBy(() -> factory.create("doc-1", -1)).isInstanceOf(RagValidationException.class);
    }
}
