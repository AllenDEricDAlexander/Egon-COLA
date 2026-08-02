package top.egon.cola.platform.idp.admin.integration.ddc;

import top.egon.cola.component.ddc.service.DdcConfigApplier;

import java.util.Objects;

public final class IdpDdcPolicyApplier implements DdcConfigApplier {

    private final String key;
    private final int priority;
    private final AtomicIdpRuntimePolicy policy;

    public IdpDdcPolicyApplier(
            String key,
            int priority,
            AtomicIdpRuntimePolicy policy
    ) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key is required");
        }
        this.key = key;
        this.priority = priority;
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    public void apply(String actualKey, String value, long version) {
        if (!key.equals(actualKey)) {
            throw new IllegalArgumentException("unexpected IdP config key");
        }
        policy.apply(actualKey, value, version);
    }

    @Override
    public int priority() {
        return priority;
    }
}
