package top.egon.cola.component.rag.chunk;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.rag.exception.RagConfigurationException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a strategy value to its implementation.
 *
 * <p>The only place a strategy is selected. A missing implementation is a configuration failure
 * rather than a silent fallback, so adding an enum value without an implementation fails at
 * start-up instead of at the first ingestion.
 */
@Slf4j
public class RagChunkingStrategyFactory {

    private final Map<RagChunkingStrategyEnum, RagChunkingStrategy> registry;

    public RagChunkingStrategyFactory(List<RagChunkingStrategy> strategies) {
        Map<RagChunkingStrategyEnum, RagChunkingStrategy> resolved = new EnumMap<>(RagChunkingStrategyEnum.class);
        if (strategies != null) {
            for (RagChunkingStrategy strategy : strategies) {
                RagChunkingStrategy previous = resolved.put(strategy.strategy(), strategy);
                if (previous != null) {
                    throw new RagConfigurationException("two chunking strategies are registered for "
                            + strategy.strategy() + ": " + previous.name() + " and " + strategy.name());
                }
            }
        }
        this.registry = Map.copyOf(resolved);
        log.info("rag chunking strategies ready: {}", registry.keySet());
    }

    /** @throws RagConfigurationException when the value has no registered implementation */
    public RagChunkingStrategy resolve(RagChunkingStrategyEnum strategy) {
        RagChunkingStrategy resolved = strategy == null ? null : registry.get(strategy);
        if (resolved == null) {
            throw new RagConfigurationException("no chunking strategy registered for " + strategy
                    + "; registered: " + registry.keySet());
        }
        return resolved;
    }
}
