package top.egon.cola.component.rag.chunk;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagChunkBO;
import top.egon.cola.component.rag.model.RagChunkingConfigDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Separator-cascade splitting.
 *
 * <p>Pieces that exceed the character budget are re-split on the next separator in the cascade;
 * when every separator is exhausted the text is cut on the character budget. The budget is derived
 * from the configured token budget using the documented characters-per-token approximation, because
 * this strategy never consults a tokenizer.
 */
@Slf4j
public class RecursiveRagChunkingStrategy implements RagChunkingStrategy {

    private static final List<String> SEPARATORS = List.of("\n\n", "\n", "。", ". ", " ");

    @Override
    public RagChunkingStrategyEnum strategy() {
        return RagChunkingStrategyEnum.RECURSIVE;
    }

    @Override
    public String name() {
        return "recursiveRagChunkingStrategy";
    }

    @Override
    public List<RagChunkBO> split(ExtractedDocumentBO document, RagChunkingConfigDTO config) {
        if (document.text().isBlank()) {
            return List.of();
        }
        try {
            List<String> pieces = new ArrayList<>();
            splitRecursively(document.text(), 0, charBudget(config.maxTokensPerChunk()), pieces);
            List<String> nonBlank = pieces.stream().filter(piece -> !piece.isBlank()).toList();
            return toChunks(applyOverlap(nonBlank, config.overlapTokens()), Map.of());
        } catch (RuntimeException exception) {
            throw failure("recursive", exception);
        }
    }

    private static void splitRecursively(String text, int separatorIndex, int budget, List<String> out) {
        if (text.isBlank()) {
            return;
        }
        if (text.length() <= budget) {
            out.add(text);
            return;
        }
        if (separatorIndex >= SEPARATORS.size()) {
            for (int start = 0; start < text.length(); start += budget) {
                out.add(text.substring(start, Math.min(text.length(), start + budget)));
            }
            return;
        }
        String separator = SEPARATORS.get(separatorIndex);
        String[] parts = text.split(Pattern.quote(separator), -1);
        if (parts.length <= 1) {
            splitRecursively(text, separatorIndex + 1, budget, out);
            return;
        }
        StringBuilder buffer = new StringBuilder();
        for (String part : parts) {
            String candidate = part + separator;
            if (buffer.length() > 0 && buffer.length() + candidate.length() > budget) {
                splitRecursively(buffer.toString(), separatorIndex + 1, budget, out);
                buffer.setLength(0);
            }
            buffer.append(candidate);
        }
        if (buffer.length() > 0) {
            splitRecursively(buffer.toString(), separatorIndex + 1, budget, out);
        }
    }
}
