package top.egon.cola.platform.idp.admin.integration.runtime;

@FunctionalInterface
public interface IdpRuntimeReadiness {

    IdpHttpProviderPublicationGate.ReadinessStatus status();
}
