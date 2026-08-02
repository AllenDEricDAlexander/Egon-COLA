package top.egon.cola.platform.idp.core.port;

public interface PasswordHashPort {

    boolean matches(char[] rawPassword, String encodedPassword);

    String encode(char[] rawPassword);

    String dummyHash();

    boolean needsUpgrade(String encodedPassword);
}
