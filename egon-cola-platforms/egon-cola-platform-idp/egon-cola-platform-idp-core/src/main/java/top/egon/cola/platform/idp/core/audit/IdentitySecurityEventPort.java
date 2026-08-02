package top.egon.cola.platform.idp.core.audit;

public interface IdentitySecurityEventPort {

    void append(IdentitySecurityEvent event);
}
