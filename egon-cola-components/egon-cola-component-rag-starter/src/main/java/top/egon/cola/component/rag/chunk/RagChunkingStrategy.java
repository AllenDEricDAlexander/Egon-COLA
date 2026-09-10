package top.egon.cola.component.rag.chunk;

import top.egon.cola.component.rag.exception.RagChunkingException;
import top.egon.cola.component.rag.exception.RagValidationException;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagChunkBO;
import top.egon.cola.component.rag.model.RagChunkingConfigDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns extracted text into ordered chunks.
 *
 * <p>Implementations must be stateless or thread safe and must be deterministic: the same document
 * and config must always produce the same chunks, because ingestion re-runs rebuild the stored
 * chunks from them.
 */
public interface RagChunkingStrategy {

    /** Characters assumed per token when a strategy needs a character budget. */
    int CHARS_PER_TOKEN = 4;

    RagChunkingStrategyEnum strategy();

    /** @throws RagValidationException for parameters this strategy cannot honour */
    List<RagChunkBO> split(ExtractedDocumentBO document, RagChunkingConfigDTO config);

    default String name() {
        return getClass().getSimpleName();
    }

    /** Character budget that approximates a token budget for character-driven strategies. */
    default int charBudget(int maxTokens) {
        return maxTokens * CHARS_PER_TOKEN;
    }

    /**
     * Prefixes each piece after the first with the tail of its predecessor.
     *
     * <p>Shared because every strategy expresses the same configured overlap the same way; keeping
     * it here avoids a helper class and keeps the behaviour identical across strategies.
     */
    default List<String> applyOverlap(List<String> pieces, int overlapTokens) {
        int overlapChars = charBudget(overlapTokens);
        if (overlapChars <= 0 || pieces.size() <= 1) {
            return List.copyOf(pieces);
        }
        List<String> overlapped = new ArrayList<>(pieces.size());
        for (int index = 0; index < pieces.size(); index++) {
            if (index == 0) {
                overlapped.add(pieces.get(index));
                continue;
            }
            String previous = pieces.get(index - 1);
            String tail = previous.length() <= overlapChars
                    ? previous
                    : previous.substring(previous.length() - overlapChars);
            overlapped.add(tail + pieces.get(index));
        }
        return List.copyOf(overlapped);
    }

    /** Wraps strategy-internal failures without leaking content into the message. */
    default RagChunkingException failure(String detail, RuntimeException cause) {
        return new RagChunkingException("chunking failed: " + detail, cause);
    }

    /** Numbers pieces in document order, which is what makes the derived chunk ids stable. */
    default List<RagChunkBO> toChunks(List<String> pieces, Map<String, String> attributes) {
        List<RagChunkBO> chunks = new ArrayList<>(pieces.size());
        for (String piece : pieces) {
            chunks.add(new RagChunkBO(chunks.size(), piece, attributes));
        }
        return List.copyOf(chunks);
    }
}
