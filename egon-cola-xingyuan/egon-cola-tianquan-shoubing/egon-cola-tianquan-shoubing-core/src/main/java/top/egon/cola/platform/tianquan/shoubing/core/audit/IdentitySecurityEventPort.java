package top.egon.cola.platform.tianquan.shoubing.core.audit;

public interface IdentitySecurityEventPort {

    void append(IdentitySecurityEvent event);
}
