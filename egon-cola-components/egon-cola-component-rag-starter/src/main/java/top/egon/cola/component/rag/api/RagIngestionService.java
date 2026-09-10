package top.egon.cola.component.rag.api;

import jakarta.validation.Valid;
import top.egon.cola.component.rag.exception.RagModelNotRegisteredException;
import top.egon.cola.component.rag.exception.RagValidationException;
import top.egon.cola.component.rag.exception.RagVectorStoreException;
import top.egon.cola.component.rag.model.RagIngestionCommand;
import top.egon.cola.component.rag.model.RagIngestionResult;

/**
 * Chunks an already extracted document, embeds it and writes it to the host's vector store.
 *
 * <p>Performs no parsing: extraction belongs to {@code RagExtractionService}. Implementations remove
 * the document's existing chunks before writing, so re-running the same document is safe and
 * produces the same chunk identities.
 *
 * <p>Embedding is performed inside the host's vector store, because the vector store SPI accepts
 * documents rather than pre-computed vectors. An embedding failure therefore surfaces as a vector
 * store failure; the component has no embedding call of its own and no separate exception for it.
 */
public interface RagIngestionService {

    /**
     * @throws RagValidationException when the command is invalid or its attributes carry a reserved
     *                                key
     * @throws RagModelNotRegisteredException when the logical model is not registered
     * @throws RagVectorStoreException when deleting the previous chunks or writing the new ones
     *                                 fails, including an embedding failure raised inside the store
     */
    RagIngestionResult ingest(@Valid RagIngestionCommand command);
}
