package top.egon.cola.component.rag.chunk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagChunkBO;
import top.egon.cola.component.rag.model.RagChunkingConfigDTO;

import java.util.List;
import java.util.Map;

/**
 * Token-budget splitting layered on the framework's token splitter.
 *
 * <p>The splitter has no overlap option, so the configured overlap is applied afterwards by
 * prefixing each piece with the tail of its predecessor.
 */
@Slf4j
public class TokenRagChunkingStrategy implements RagChunkingStrategy {

    /** Guard against a pathological document producing an unbounded number of chunks. */
    private static final int MAX_NUM_CHUNKS = 10_000;

    @Override
    public RagChunkingStrategyEnum strategy() {
        return RagChunkingStrategyEnum.TOKEN;
    }

    @Override
    public String name() {
        return "tokenRagChunkingStrategy";
    }

    @Override
    public List<RagChunkBO> split(ExtractedDocumentBO document, RagChunkingConfigDTO config) {
        if (document.text().isBlank()) {
            return List.of();
        }
        try {
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(config.maxTokensPerChunk())
                    .withMinChunkSizeChars(config.minChunkChars())
                    .withMinChunkLengthToEmbed(1)
                    .withMaxNumChunks(MAX_NUM_CHUNKS)
                    .withKeepSeparator(true)
                    .build();
            List<String> pieces = splitter.apply(List.of(new Document(document.text()))).stream()
                    .map(Document::getText)
                    .filter(piece -> !piece.isBlank())
                    .toList();
            return toChunks(applyOverlap(pieces, config.overlapTokens()), Map.of());
        } catch (RuntimeException exception) {
            throw failure("token", exception);
        }
    }
}
