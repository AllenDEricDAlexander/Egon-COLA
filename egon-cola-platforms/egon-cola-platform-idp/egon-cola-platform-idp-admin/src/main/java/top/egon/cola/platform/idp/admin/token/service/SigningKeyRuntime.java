package top.egon.cola.platform.idp.admin.token.service;

public interface SigningKeyRuntime {

    void activate(String kid);

    boolean isServing(String kid);
}
