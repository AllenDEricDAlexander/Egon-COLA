package top.egon.cola.component.rag.converter;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import top.egon.cola.component.rag.metadata.RagMetadataKeys;
import top.egon.cola.component.rag.model.RagChunkBO;
import top.egon.cola.component.rag.model.RagRetrievedChunkBO;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Locks the bidirectional mapping and the metadata split between reserved and business keys. */
class RagChunkConverterTest {

    @Test
    void builds_a_vector_document_from_a_chunk() {
        Document document = RagChunkConverter.INSTANCE.toDocument(
                new RagChunkBO(7, "chunk body", Map.of("source", "manual")), "doc-1:7",
                metadata());

        assertThat(document.getId()).isEqualTo("doc-1:7");
        assertThat(document.getText()).isEqualTo("chunk body");
        assertThat(document.getMetadata()).containsEntry("chunkIndex", 7).containsEntry("source", "manual");
    }

    @Test
    void maps_a_vector_document_back_to_a_chunk() {
        Document document = new Document("doc-1:7", "chunk body", metadata()).mutate().score(0.83).build();

        RagRetrievedChunkBO chunk = RagChunkConverter.INSTANCE.toSource(document);

        assertThat(chunk.chunkId()).isEqualTo("doc-1:7");
        assertThat(chunk.content()).isEqualTo("chunk body");
        assertThat(chunk.collectionId()).isEqualTo("kb-1");
        assertThat(chunk.documentId()).isEqualTo("doc-1");
        assertThat(chunk.chunkIndex()).isEqualTo(7);
        assertThat(chunk.logicalModelName()).isEqualTo("openai-small");
    }

    @Test
    void keeps_only_business_metadata_in_the_attributes() {
        Document document = new Document("doc-1:7", "chunk body", metadata());

        RagRetrievedChunkBO chunk = RagChunkConverter.INSTANCE.toSource(document);

        assertThat(chunk.attributes()).containsOnlyKeys("source");
    }

    @Test
    void keeps_the_score_null_when_the_store_does_not_provide_one() {
        Document document = new Document("doc-1:7", "chunk body", metadata());

        assertThat(RagChunkConverter.INSTANCE.toSource(document).score()).isNull();
    }

    @Test
    void rebuilds_a_vector_document_from_a_retrieved_chunk() {
        RagRetrievedChunkBO chunk = new RagRetrievedChunkBO("doc-1:7", "kb-1", "doc-1", 7, "chunk body", 0.83,
                "openai-small", Map.of("source", "manual"));

        Document document = RagChunkConverter.INSTANCE.toTarget(chunk);

        assertThat(document.getId()).isEqualTo("doc-1:7");
        assertThat(document.getText()).isEqualTo("chunk body");
        assertThat(document.getMetadata()).containsEntry("chunkIndex", 7).containsEntry("source", "manual");
    }

    private static Map<String, Object> metadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(RagMetadataKeys.COLLECTION_ID, "kb-1");
        metadata.put(RagMetadataKeys.DOCUMENT_ID, "doc-1");
        metadata.put(RagMetadataKeys.CHUNK_INDEX, 7);
        metadata.put(RagMetadataKeys.LOGICAL_MODEL_NAME, "openai-small");
        metadata.put("source", "manual");
        return metadata;
    }
}
