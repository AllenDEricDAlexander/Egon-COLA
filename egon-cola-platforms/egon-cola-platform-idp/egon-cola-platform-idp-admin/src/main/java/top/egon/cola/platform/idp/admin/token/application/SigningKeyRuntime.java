package top.egon.cola.platform.idp.admin.token.application;

public interface SigningKeyRuntime {

    void activate(String kid);

    boolean isServing(String kid);
}
