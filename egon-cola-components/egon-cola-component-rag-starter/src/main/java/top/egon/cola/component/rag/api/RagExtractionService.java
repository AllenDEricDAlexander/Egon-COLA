package top.egon.cola.component.rag.api;

import jakarta.validation.Valid;
import top.egon.cola.component.rag.exception.RagExtractionException;
import top.egon.cola.component.rag.exception.RagExtractorConflictException;
import top.egon.cola.component.rag.exception.RagExtractorMissingException;
import top.egon.cola.component.rag.exception.RagValidationException;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagExtractionCommand;

/**
 * Turns a byte stream into text and structural metadata.
 *
 * <p>Separated from ingestion on purpose: a consumer normally has to persist the extracted text
 * before triggering embedding, so that a retried embedding never re-reads or re-parses the original
 * document. This method performs no chunking, no embedding and no persistence.
 */
public interface RagExtractionService {

    /**
     * @throws RagValidationException when content is null or an extractor returns a reserved
     *                                metadata key
     * @throws RagExtractorMissingException when no registered extractor accepts the format; the
     *                                      message lists the registered capabilities
     * @throws RagExtractorConflictException when two extractors share an order and a format
     * @throws RagExtractionException when the selected extractor fails to parse the document
     */
    ExtractedDocumentBO extract(@Valid RagExtractionCommand command);
}
