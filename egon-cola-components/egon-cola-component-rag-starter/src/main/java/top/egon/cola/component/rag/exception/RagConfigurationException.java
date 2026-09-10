package top.egon.cola.component.rag.exception;

/** Configuration is missing, unknown or out of range; the context must fail closed. */
public class RagConfigurationException extends RagException {

    public RagConfigurationException(String safeMessage) {
        super("RAG_CONFIGURATION", safeMessage);
    }
}
