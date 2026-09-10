package top.egon.cola.component.rag.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.rag.api.RagExtractionService;
import top.egon.cola.component.rag.exception.RagValidationException;
import top.egon.cola.component.rag.extract.RagDocumentExtractor;
import top.egon.cola.component.rag.extract.RagDocumentExtractorRegistry;
import top.egon.cola.component.rag.metadata.RagMetadataKeys;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagExtractionCommand;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Routes to a registered extractor and validates what comes back.
 *
 * <p>Logs only identifiers, the selected extractor, the text length and the duration; document
 * content never reaches the log.
 */
@Slf4j
@RequiredArgsConstructor
public class RagExtractionServiceImpl implements RagExtractionService {

    private final RagDocumentExtractorRegistry extractorRegistry;

    private final Clock clock;

    @Override
    public ExtractedDocumentBO extract(RagExtractionCommand command) {
        if (command == null || command.content() == null) {
            throw new RagValidationException("content must not be null");
        }
        Instant startedAt = clock.instant();
        RagDocumentExtractor extractor = extractorRegistry.route(command.mimeType(), command.fileName());
        log.info("rag extraction started: file={}, mime={}, extractor={}",
                command.fileName(), command.mimeType(), extractor.name());

        ExtractedDocumentBO document = extractor.extract(command.content(), command.mimeType(), command.fileName());
        RagMetadataKeys.rejectReservedKeys(document.attributes());

        log.info("rag extraction finished: extractor={}, textChars={}, durationMs={}, result=SUCCESS",
                extractor.name(), document.text().length(),
                Duration.between(startedAt, clock.instant()).toMillis());
        return document;
    }
}
