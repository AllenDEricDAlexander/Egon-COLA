package top.egon.cola.component.gateway.engine.discovery;

public enum ProviderCandidateStage {

    EXACT_SERVICE,

    VALID_LEASE,

    PROTOCOL_MATCH,

    ADMIN_ENABLED,

    LOCATION_AND_TAGS,

    HEALTHY,

    ADMISSION_AVAILABLE,

    POSITIVE_WEIGHT
}
