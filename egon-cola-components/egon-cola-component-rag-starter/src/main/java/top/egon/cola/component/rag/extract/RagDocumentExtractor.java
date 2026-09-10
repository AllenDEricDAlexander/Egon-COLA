package top.egon.cola.component.rag.extract;

import top.egon.cola.component.rag.exception.RagExtractionException;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;

import java.io.InputStream;
import java.util.Set;

/**
 * Turns a byte stream plus format hints into text and structural metadata.
 *
 * <p>Implementations must be stateless or thread safe, must tolerate both hints being {@code null},
 * must read the stream once and must never close it. Returning {@code true} from {@link #supports}
 * for everything is allowed only for a lowest-priority catch-all implementation.
 */
public interface RagDocumentExtractor {

    /** Whether this implementation can parse the described content. */
    boolean supports(String mimeType, String fileName);

    /** @throws RagExtractionException when the content cannot be parsed */
    ExtractedDocumentBO extract(InputStream content, String mimeType, String fileName);

    /** Priority; the lowest value wins. Defaults to a generic priority. */
    default int order() {
        return 100;
    }

    /** Stable implementation name used in diagnostics and conflict messages. */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Formats this implementation claims, used for static ambiguity checks and for the diagnostic
     * message when nothing matches. An empty set means "unknown", which never participates in a
     * conflict.
     */
    default Set<String> declaredMimeTypes() {
        return Set.of();
    }
}
