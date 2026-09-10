package top.egon.cola.component.rag.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import top.egon.cola.component.rag.exception.RagValidationException;
import top.egon.cola.component.rag.metadata.RagMetadataKeys;

import java.util.Map;

/**
 * Read intent for {@code RagRetrievalService#retrieve}.
 *
 * <p>{@code attributes} are equality conditions added on top of the collection and model filters the
 * component always applies; they can never replace them, and a reserved key is rejected here.
 */
public record RagRetrievalQuery(@NotBlank String collectionId,
                                @NotBlank String logicalModelName,
                                @NotBlank @Size(max = 2000) String query,
                                int topK,
                                double similarityThreshold,
                                Map<String, String> attributes) {

    public RagRetrievalQuery {
        collectionId = blankToNull(collectionId);
        logicalModelName = blankToNull(logicalModelName);
        query = blankToNull(query);
        topK = Math.max(topK, 0);
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new RagValidationException("similarityThreshold must be between 0.0 and 1.0");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        RagMetadataKeys.rejectReservedKeys(attributes);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
