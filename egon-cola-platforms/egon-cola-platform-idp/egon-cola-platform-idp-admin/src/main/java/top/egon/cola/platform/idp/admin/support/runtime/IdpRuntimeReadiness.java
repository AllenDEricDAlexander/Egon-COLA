package top.egon.cola.platform.idp.admin.support.runtime;

@FunctionalInterface
public interface IdpRuntimeReadiness {

    IdpHttpProviderPublicationGate.ReadinessStatus status();
}
