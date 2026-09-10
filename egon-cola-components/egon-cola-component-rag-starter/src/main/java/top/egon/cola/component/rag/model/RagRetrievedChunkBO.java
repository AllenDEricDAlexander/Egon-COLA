package top.egon.cola.component.rag.model;

import java.util.Map;

/**
 * One retrieved chunk together with its score.
 *
 * <p>{@code score} is nullable because the underlying vector store is not required to return one.
 */
public record RagRetrievedChunkBO(String chunkId,
                                  String collectionId,
                                  String documentId,
                                  int chunkIndex,
                                  String content,
                                  Double score,
                                  String logicalModelName,
                                  Map<String, String> attributes) {

    public RagRetrievedChunkBO {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
