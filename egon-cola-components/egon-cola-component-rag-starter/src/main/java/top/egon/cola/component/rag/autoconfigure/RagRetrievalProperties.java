package top.egon.cola.component.rag.autoconfigure;

import top.egon.cola.component.rag.exception.RagConfigurationException;

/** Bounds applied to every retrieval call. */
public record RagRetrievalProperties(int defaultTopK, int maxTopK) {

    public static final int DEFAULT_TOP_K = 8;

    public static final int DEFAULT_MAX_TOP_K = 50;

    private static final int ABSOLUTE_MAX_TOP_K = 200;

    public RagRetrievalProperties {
        maxTopK = maxTopK <= 0 ? DEFAULT_MAX_TOP_K : maxTopK;
        defaultTopK = defaultTopK <= 0 ? DEFAULT_TOP_K : defaultTopK;
        if (maxTopK > ABSOLUTE_MAX_TOP_K) {
            throw new RagConfigurationException("max-top-k must not exceed " + ABSOLUTE_MAX_TOP_K);
        }
        if (defaultTopK > maxTopK) {
            throw new RagConfigurationException("default-top-k must not exceed max-top-k");
        }
    }
}
