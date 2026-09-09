package top.egon.cola.platform.tianquan.shoubing.admin.support.runtime;

@FunctionalInterface
public interface IdpRuntimeReadiness {

    IdpHttpProviderPublicationGate.ReadinessStatus status();
}
