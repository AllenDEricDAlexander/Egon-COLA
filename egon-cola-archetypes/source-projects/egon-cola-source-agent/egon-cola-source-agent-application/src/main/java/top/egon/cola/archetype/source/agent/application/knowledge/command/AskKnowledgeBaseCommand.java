package top.egon.cola.archetype.source.agent.application.knowledge.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.regex.Pattern;

/**
 * Normalized question intent for one knowledge base.
 *
 * <p>Carries the question and the citation cap only: the answer is produced from the chunks the use
 * case retrieves, so the model name, the collection and the filter are never caller input.
 */
public record AskKnowledgeBaseCommand(
        @NotNull @Positive Long knowledgeBaseId,
        @NotBlank @Size(max = 2000) String question,
        @Min(1) @Max(50) Integer topK,
        @NotBlank @Size(max = 128) String traceId) {

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("\\p{Cntrl}");

    public AskKnowledgeBaseCommand {
        question = normalize(question);
        traceId = normalize(traceId);
    }

    /** @return whether the question carries a control character the contract rejects */
    public boolean hasControlCharacters() {
        return question != null && CONTROL_CHARACTERS.matcher(question).find();
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
