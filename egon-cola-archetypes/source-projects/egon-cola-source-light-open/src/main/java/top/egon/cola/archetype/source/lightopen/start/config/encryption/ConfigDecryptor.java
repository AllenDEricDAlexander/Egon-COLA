package top.egon.cola.archetype.source.lightopen.start.config.encryption;

public interface ConfigDecryptor {

    boolean supports(String value);

    String decrypt(String value, char[] key);
}
