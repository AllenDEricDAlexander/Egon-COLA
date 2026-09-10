package top.egon.cola.archetype.source.agent.application.knowledge.command;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.regex.Pattern;

/**
 * Normalized retrieval-debug intent for one knowledge base.
 *
 * <p>The caller owns the query, the result cap and the score floor; the collection, the logical
 * model and the tenant filter are derived from the base by the use case and cannot be supplied.
 * {@code topK} and {@code similarityThreshold} are nullable so that "absent" stays distinguishable
 * from an explicit value and the component default applies.
 */
public record RetrieveKnowledgeCommand(
        @NotNull @Positive Long knowledgeBaseId,
        @NotBlank @Size(max = 2000) String query,
        @Min(1) @Max(50) Integer topK,
        @DecimalMin("0.0") @DecimalMax("1.0") Double similarityThreshold,
        @NotBlank @Size(max = 128) String traceId) {

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("\\p{Cntrl}");

    public RetrieveKnowledgeCommand {
        query = normalize(query);
        traceId = normalize(traceId);
    }

    /** The score floor, where an absent value accepts every score. */
    public double effectiveSimilarityThreshold() {
        return similarityThreshold == null ? 0.0d : similarityThreshold;
    }

    /** @return whether the query carries a control character the contract rejects */
    public boolean hasControlCharacters() {
        return query != null && CONTROL_CHARACTERS.matcher(query).find();
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
