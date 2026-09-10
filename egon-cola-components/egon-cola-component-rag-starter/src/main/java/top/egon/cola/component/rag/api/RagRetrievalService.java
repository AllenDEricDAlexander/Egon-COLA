package top.egon.cola.component.rag.api;

import jakarta.validation.Valid;
import top.egon.cola.component.rag.exception.RagModelNotRegisteredException;
import top.egon.cola.component.rag.exception.RagValidationException;
import top.egon.cola.component.rag.exception.RagVectorStoreException;
import top.egon.cola.component.rag.model.RagRetrievalQuery;
import top.egon.cola.component.rag.model.RagRetrievedChunkBO;

import java.util.List;

/**
 * Retrieves the chunks of one collection that were written by one logical model.
 *
 * <p>The collection and model filters are injected by the component and cannot be omitted or
 * overridden: the vector table is shared, so a missing filter would silently return another
 * collection's or another model's chunks, which no caller could detect afterwards.
 */
public interface RagRetrievalService {

    /**
     * @return matching chunks ordered by descending score, with an unknown score last; an empty
     *         list when nothing matches
     * @throws RagValidationException when a parameter is out of range or an attribute carries a
     *                                reserved key
     * @throws RagModelNotRegisteredException when the logical model is not registered
     * @throws RagVectorStoreException when the search fails; never degraded to an empty result
     */
    List<RagRetrievedChunkBO> retrieve(@Valid RagRetrievalQuery query);
}
