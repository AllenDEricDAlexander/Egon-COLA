package top.egon.cola.component.rag.storage;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.rag.autoconfigure.RagStorageProperties;
import top.egon.cola.component.rag.exception.RagConfigurationException;

/**
 * Fails start-up when the configured storage type and the registered implementation disagree.
 *
 * <p>Exists as its own bean so the check still runs when the host replaces the built-in storage: a
 * host bean of the local type while the configuration claims another backing would otherwise be
 * discovered only when a document is first written.
 */
@Slf4j
public class RagStorageTypeGuard {

    public RagStorageTypeGuard(RagStorageProperties properties, RagDocumentStorage storage) {
        if (properties.type() != storage.type()) {
            throw new RagConfigurationException("storage.type is " + properties.type()
                    + " but the registered RagDocumentStorage reports " + storage.type());
        }
        log.info("rag document storage ready: {} with {}", storage.type(), storage.getClass().getSimpleName());
    }
}
