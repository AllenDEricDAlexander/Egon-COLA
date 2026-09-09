package top.egon.cola.platform.tianquan.shoubing.admin.token.service;

public interface SigningKeyRuntime {

    void activate(String kid);

    boolean isServing(String kid);
}
