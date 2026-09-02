package top.egon.cola.archetype.source.web.starter.config.encryption;

public interface ConfigDecryptor {

    boolean supports(String value);

    String decrypt(String value, char[] key);
}
