package top.egon.cola.component.rag.autoconfigure;

/**
 * Start-up validation options.
 *
 * <p>The probe is off by default: it performs a real round trip against the embedding model and the
 * vector store, so enabling it makes provider availability and a small amount of billing a
 * prerequisite of application start-up.
 */
public record RagValidationProperties(boolean probeOnStartup) {
}
