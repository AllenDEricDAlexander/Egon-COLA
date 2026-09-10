package top.egon.cola.component.rag.extract;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.rag.exception.RagExtractorConflictException;
import top.egon.cola.component.rag.exception.RagExtractorMissingException;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Routes a format to exactly one extractor.
 *
 * <p>Implementations are ordered by {@link RagDocumentExtractor#order()}; the first one whose
 * {@code supports} accepts the format wins and the choice is logged. Two implementations that share
 * an order and a declared MIME type are rejected at construction, because at that point the choice
 * would depend on bean ordering rather than on an explicit priority.
 */
@Slf4j
public class RagDocumentExtractorRegistry {

    private final List<RagDocumentExtractor> extractors;

    public RagDocumentExtractorRegistry(List<RagDocumentExtractor> extractors) {
        this.extractors = extractors == null
                ? List.of()
                : extractors.stream().sorted(Comparator.comparingInt(RagDocumentExtractor::order)).toList();
        rejectAmbiguousPriorities();
        log.info("rag document extractors ready: {}",
                this.extractors.stream().map(RagDocumentExtractor::name).toList());
    }

    /** @throws RagExtractorMissingException when no registered extractor accepts the format */
    public RagDocumentExtractor route(String mimeType, String fileName) {
        return extractors.stream()
                .filter(extractor -> extractor.supports(mimeType, fileName))
                .findFirst()
                .map(extractor -> {
                    log.debug("routed mime '{}' and file '{}' to extractor {}",
                            mimeType, fileName, extractor.name());
                    return extractor;
                })
                .orElseThrow(() -> new RagExtractorMissingException("no document extractor supports mime '"
                        + mimeType + "' and file '" + fileName + "'; registered capabilities: "
                        + registeredMimeCapabilities()));
    }

    /** Union of every declared MIME type; used in diagnostics, never for routing. */
    public Set<String> registeredMimeCapabilities() {
        Set<String> capabilities = new TreeSet<>();
        extractors.forEach(extractor -> capabilities.addAll(extractor.declaredMimeTypes()));
        return capabilities;
    }

    private void rejectAmbiguousPriorities() {
        for (int left = 0; left < extractors.size(); left++) {
            for (int right = left + 1; right < extractors.size(); right++) {
                RagDocumentExtractor first = extractors.get(left);
                RagDocumentExtractor second = extractors.get(right);
                if (first.order() != second.order()) {
                    continue;
                }
                Set<String> overlap = new TreeSet<>(first.declaredMimeTypes());
                overlap.retainAll(second.declaredMimeTypes());
                if (!overlap.isEmpty()) {
                    throw new RagExtractorConflictException("document extractors '" + first.name() + "' and '"
                            + second.name() + "' share order " + first.order()
                            + " and claim the same formats: " + overlap);
                }
            }
        }
    }
}
