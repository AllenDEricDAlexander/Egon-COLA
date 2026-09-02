package top.egon.cola.archetype.source.light.start.config.encryption;

public class ConfigDecryptException extends RuntimeException {

    public ConfigDecryptException(String message) {
        super(message);
    }

    public ConfigDecryptException(String message, Throwable cause) {
        super(message, cause);
    }
}
