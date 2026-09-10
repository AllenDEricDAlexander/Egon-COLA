package top.egon.cola.component.rag.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import top.egon.cola.component.rag.autoconfigure.RagEmbeddingModelProperties;
import top.egon.cola.component.rag.autoconfigure.RagProperties;
import top.egon.cola.component.rag.embed.RagEmbeddingModelRegistry;
import top.egon.cola.component.rag.exception.RagConfigurationException;
import top.egon.cola.component.rag.support.FakeEmbeddingModel;
import top.egon.cola.component.rag.support.FakeVectorStore;

import java.time.Clock;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Locks the off-by-default probe: it must be silent when off and fail closed when on. */
class RagVectorStoreProbeTest {

    private static final int DIMENSIONS = 1536;

    private FakeVectorStore vectorStore;

    private RagVectorStoreProbe probe;

    @BeforeEach
    void setUp() {
        vectorStore = new FakeVectorStore();
        probe = new RagVectorStoreProbe(vectorStore, Clock.systemUTC());
    }

    @Test
    void succeeds_against_a_working_vector_store_and_leaves_nothing_behind() {
        assertThatCode(() -> probe.validate(registry())).doesNotThrowAnyException();

        assertThat(vectorStore.searchRequests()).hasSize(1);
        // The probe removes its record by identifier, so an empty store proves the cleanup ran.
        assertThat(vectorStore.addedDocuments()).isEmpty();
    }

    @Test
    void probes_the_documented_reserved_collection() {
        probe.validate(registry());

        assertThat(vectorStore.searchRequests().get(0).getFilterExpression().toString())
                .contains(RagVectorStoreProbe.PROBE_COLLECTION);
    }

    @Test
    void fails_closed_when_the_search_fails() {
        vectorStore.failOn(FakeVectorStore.FailurePoint.SEARCH);

        assertThatThrownBy(() -> probe.validate(registry()))
                .isInstanceOf(RagConfigurationException.class)
                .hasMessageContaining("openai-small")
                .hasMessageContaining("search");
    }

    @Test
    void fails_closed_when_the_write_fails() {
        vectorStore.failOn(FakeVectorStore.FailurePoint.ADD);

        assertThatThrownBy(() -> probe.validate(registry()))
                .isInstanceOf(RagConfigurationException.class)
                .hasMessageContaining("openai-small")
                .hasMessageContaining("add");
    }

    private RagEmbeddingModelRegistry registry() {
        RagProperties properties = new RagProperties(true, DIMENSIONS, "hostVectorStore",
                Map.of("openai-small", new RagEmbeddingModelProperties("ragEmbeddingModel")), null, null, null, null);
        return new RagEmbeddingModelRegistry(properties,
                new StaticListableBeanFactory(Map.of("ragEmbeddingModel", new FakeEmbeddingModel(DIMENSIONS))));
    }
}
