package top.egon.cola.archetype.source.agent.adapter.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One citation of a streamed answer (Spec B §9.2.12).
 *
 * <p>It names the chunk without carrying its text: the references exist so a client can point at the
 * sources of an answer, and the text they were drawn from is retrievable through the retrieval
 * endpoint when it is actually wanted. Sending it on every event would repeat the whole corpus of a
 * question inside the stream.
 */
@Schema(name = "KnowledgeQaReferenceVO")
public record KnowledgeQaReferenceVO(
        @Schema(example = "9") Long documentId,
        @Schema(example = "7", minimum = "0") int chunkIndex,
        @Schema(example = "report.pdf") String displayName,
        @Schema(example = "0.83", nullable = true,
                description = "Similarity as the vector store reported it; absent when it ranks only")
        Double score) {
}
